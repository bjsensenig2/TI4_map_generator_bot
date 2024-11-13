package ti4.commands.units;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.StringUtils;
import ti4.commands.GameStateCommand;
import ti4.commands.planet.PlanetAdd;
import ti4.generator.Mapper;
import ti4.generator.TileHelper;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.helpers.UnitParser;
import ti4.helpers.Units.UnitKey;
import ti4.helpers.Units.UnitType;
import ti4.map.Game;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.message.MessageHelper;

abstract public class AddRemoveUnits extends GameStateCommand {

    public AddRemoveUnits() {
        super(true, true);
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, Constants.TILE_NAME, "System/Tile name"),
                new OptionData(OptionType.STRING, Constants.UNIT_NAMES, "Comma separated list of '{count} unit {planet}' Eg. 2 infantry primor, carrier, 2 fighter, mech pri").setRequired(true),
                new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for unit").setAutoComplete(true),
                new OptionData(OptionType.BOOLEAN, Constants.NO_MAPGEN, "'True' to not generate a map update with this command"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getGame();
        String color = Helper.getColor(game, event);
        if (!Mapper.isValidColor(color)) {
            MessageHelper.replyToMessage(event, "Color/Faction not valid");
            return;
        }

        OptionMapping option = event.getOption(Constants.TILE_NAME);
        String tileOption = option != null ?
                StringUtils.substringBefore(event.getOption(Constants.TILE_NAME, null, OptionMapping::getAsString).toLowerCase(), " ")
                : "nombox";
        String tileID = AliasHandler.resolveTile(tileOption);
        Tile tile = TileHelper.getTileObject(event, tileID, game);
        if (tile == null)
            return;

        UnitParser.unitParsingForTile(event, color, tile, game);
        for (UnitHolder unitHolder_ : tile.getUnitHolders().values()) {
            addPlanetToPlayArea(event, tile, unitHolder_.getName());
        }
        actionAfterAll(event, tile, color, game);
    }

    protected void addPlanetToPlayArea(GenericInteractionCreateEvent event, Tile tile, String planetName) {
        if (Constants.SPACE.equals(planetName)) {
            return;
        }
        UnitHolder unitHolder = tile.getUnitHolders().get(planetName);
        if (unitHolder == null) {
            return;
        }
        Set<UnitKey> allUnitsOnPlanet = unitHolder.getUnits().keySet();
        Set<String> unitColors = new HashSet<>();
        for (UnitKey unit_ : allUnitsOnPlanet) {
            String unitColor = unit_.getColorID();
            if (unit_.getUnitType() != UnitType.Fighter) {
                unitColors.add(unitColor);
            }
        }

        var game = getGame();
        if (unitColors.size() == 1) {
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

    protected void unitParsingForTile(SlashCommandInteractionEvent event, String color, Tile tile, Game game) {
        UnitParser.unitParsingForTile(event, color, tile, game);
    }

    protected void u(SlashCommandInteractionEvent event, String color, Tile tile, Game game) {
        UnitParser.unitParsingForTile(event, color, tile, game);
    }

    public static String getPlanet(Tile tile, String planetName) {
        if (tile.isSpaceHolderValid(planetName)) {
            return planetName;
        }
        return tile.getUnitHolders().keySet().stream()
            .filter(id -> !Constants.SPACE.equals(planetName))
            .filter(unitHolderID -> unitHolderID.startsWith(planetName))
            .findFirst().orElse(planetName);
    }

    abstract protected void unitAction(SlashCommandInteractionEvent event, Tile tile, int count, String planetName,
        UnitKey unitID, String color, Game game);

    abstract protected void unitAction(GenericInteractionCreateEvent event, Tile tile, int count, String planetName,
        UnitKey unitID, String color, Game game);

    protected void actionAfterAll(SlashCommandInteractionEvent event, Tile tile, String color, Game game) {
        // do nothing, overriden by child classes
    }
}
