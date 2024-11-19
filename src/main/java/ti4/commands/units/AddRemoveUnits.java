package ti4.commands.units;

import java.util.HashSet;
import java.util.Set;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.lang3.StringUtils;
import ti4.commands.planet.PlanetAdd;
import ti4.commands2.CommandHelper;
import ti4.commands2.GameStateCommand;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.helpers.Units.UnitKey;
import ti4.helpers.Units.UnitType;
import ti4.image.Mapper;
import ti4.image.TileHelper;
import ti4.map.Game;
import ti4.map.GameManager;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.message.MessageHelper;
import ti4.service.ShowGameService;

abstract public class AddRemoveUnits extends GameStateCommand {

    public AddRemoveUnits() {
        super(true, false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getGame();
        // can be a color not in the game for neutral units
        String color = getColor(game, event);
        if (!Mapper.isValidColor(color)) {
            MessageHelper.replyToMessage(event, "Color/Faction not valid");
            return;
        }

        String tileOption = StringUtils.substringBefore(event.getOption(Constants.TILE_NAME).getAsString().toLowerCase(), " ");
        String tileId = AliasHandler.resolveTile(tileOption);
        Tile tile = getTileObject(event, tileId, game);
        if (tile == null)
            return;

        unitParsingForTile(event, color, tile, game);

        for (UnitHolder unitHolder_ : tile.getUnitHolders().values()) {
            addPlanetToPlayArea(event, tile, unitHolder_.getName(), game);
        }
        new AddUnits().actionAfterAll(event, tile, color, game); // TODO: shouldn't do this, instantiating subclass here

        boolean generateMap = !event.getOption(Constants.NO_MAPGEN, false, OptionMapping::getAsBoolean);
        if (generateMap) {
            ShowGameService.simpleShowGame(game, event);
        } else {
            MessageHelper.replyToMessage(event, "Map update completed");
        }
    }

    protected Tile getTileObject(SlashCommandInteractionEvent event, String tileID, Game game) {
        return TileHelper.getTile(event, tileID, game);
    }

    protected void unitParsingForTile(SlashCommandInteractionEvent event, String color, Tile tile, Game game) {
        String unitList = event.getOption(Constants.UNIT_NAMES).getAsString().toLowerCase();

        if (game.getPlayerFromColorOrFaction(color) == null && !game.getPlayerIDs().contains(Constants.dicecordId)) {
            game.setupNeutralPlayer(color);
        }

        unitParsing(event, color, tile, unitList, game);
    }

    protected void unitParsing(SlashCommandInteractionEvent event, String color, Tile tile, String unitList, Game game) {
        if (game.getPlayerFromColorOrFaction(color) == null && !game.getPlayerIDs().contains(Constants.dicecordId)) {
            game.setupNeutralPlayer(color);
        }

        commonUnitParsing(event, color, tile, unitList, game);
        actionAfterAll(event, tile, color, game);
    }

    protected void unitParsing(GenericInteractionCreateEvent event, String color, Tile tile, String unitList, Game game) {
        unitList = unitList.replace(", ", ",").replace("-", "").replace("'", "").toLowerCase();
        if (!Mapper.isValidColor(color)) {
            return;
        }
        if (game.getPlayerFromColorOrFaction(color) == null && !game.getPlayerIDs().contains(Constants.dicecordId)) {
            game.setupNeutralPlayer(color);
        }

        commonUnitParsing(event, color, tile, unitList, game);
    }

    protected String recheckColorForUnit(String unit, String color, GenericInteractionCreateEvent event) {
        return color;
    }



    protected static void addPlanetToPlayArea(GenericInteractionCreateEvent event, Tile tile, String planetName, Game game) {
        if (Constants.SPACE.equals(planetName)) {
            return;
        }
        String userID = event.getUser().getId();
        if (game == null) {
            game = GameManager.getUserActiveGame(userID);
        }
        UnitHolder unitHolder = tile.getUnitHolders().get(planetName);
        if (unitHolder != null) {
            Set<UnitKey> allUnitsOnPlanet = unitHolder.getUnits().keySet();
            Set<String> unitColors = new HashSet<>();
            for (UnitKey unit_ : allUnitsOnPlanet) {
                String unitColor = unit_.getColorID();
                if (unit_.getUnitType() != UnitType.Fighter) {
                    unitColors.add(unitColor);
                }
            }

            if (unitColors.size() != 1) {
                return;
            }
            String unitColor = unitColors.iterator().next();
            for (Player player : game.getPlayers().values()) {
                if (player.getFaction() != null && player.getColor() != null) {
                    String colorID = Mapper.getColorID(player.getColor());
                    if (unitColor.equals(colorID)) {
                        if (!player.getPlanetsAllianceMode().contains(planetName)) {
                            PlanetAdd.doAction(player, planetName, game, event, false);
                        }
                        break;
                    }
                }
            }
        }
    }

    abstract protected void unitAction(SlashCommandInteractionEvent event, Tile tile, int count, String planetName,
        UnitKey unitID, String color, Game game);

    abstract protected void unitAction(GenericInteractionCreateEvent event, Tile tile, int count, String planetName,
        UnitKey unitID, String color, Game game);

    protected void actionAfterAll(SlashCommandInteractionEvent event, Tile tile, String color, Game game) {
        // do nothing, overriden by child classes
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void register(CommandListUpdateAction commands) {
        commands.addCommands(
            Commands.slash(getName(), getDescription())
                .addOptions(
                    new OptionData(OptionType.STRING, Constants.TILE_NAME, "System/Tile name")
                        .setRequired(true)
                        .setAutoComplete(true),
                    new OptionData(OptionType.STRING, Constants.UNIT_NAMES, "Comma separated list of '{count} unit {planet}' Eg. 2 infantry primor, carrier, 2 fighter, mech pri")
                        .setRequired(true),
                    new OptionData(OptionType.STRING, Constants.COLOR, "Color for unit")
                        .setAutoComplete(true),
                    new OptionData(OptionType.BOOLEAN, Constants.NO_MAPGEN, "'True' to not generate a map update with this command")));
    }

    private static String getColor(Game game, SlashCommandInteractionEvent event) {
        OptionMapping factionColorOption = event.getOption(Constants.COLOR);
        if (factionColorOption != null) {
            String colorFromString = CommandHelper.getColorFromString(game, factionColorOption.getAsString());
            if (Mapper.isValidColor(colorFromString)) {
                return colorFromString;
            }
        } else {
            Player foundPlayer = CommandHelper.getPlayerFromGame(game, event.getMember(), event.getUser().getId());
            if (foundPlayer != null) {
                return foundPlayer.getColor();
            }
        }
        return null;
    }
}
