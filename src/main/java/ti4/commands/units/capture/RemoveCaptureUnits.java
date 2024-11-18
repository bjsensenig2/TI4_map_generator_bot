package ti4.commands.units.capture;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands2.GameStateSubcommand;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.Player;
import ti4.map.Tile;

class RemoveCaptureUnits extends GameStateSubcommand {

    public RemoveCaptureUnits() {
        super(Constants.REMOVE_UNITS, "Release units", true, true);
    }

    protected void options() {
        addOptions(new OptionData(OptionType.STRING, Constants.UNIT_NAMES, "Comma separated list of '{count} unit' Eg. 2 infantry, carrier, 2 fighter, mech").setRequired(true));
        addOptions(new OptionData(OptionType.STRING, Constants.TARGET_FACTION_OR_COLOR, "Faction or Color for unit").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Captor's faction or color (defaults to you))").setAutoComplete(true));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.NO_MAPGEN, "'True' to not generate a map update with this command"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Player player = getPlayer();
        Tile tile = player.getNomboxTile();
        subExecute(event, tile);
    }

    // TODO: this feels super hacky and would maybe be better re-written...
    private void subExecute(SlashCommandInteractionEvent event, Tile tile) {
        ti4.commands.units.RemoveUnits removeUnits = new ti4.commands.units.RemoveUnits() {
            @Override
            public Tile getTileObject(SlashCommandInteractionEvent event, String tileID, Game game) {
                return tile;
            }

            @Override
            protected String recheckColorForUnit(String unit, String color, GenericInteractionCreateEvent event) {
                if (unit.contains("ff") || unit.contains("gf")) {
                    return getPlayer().getColor();
                }
                return color;
            }
        };
        removeUnits.execute(event);
    }
}
