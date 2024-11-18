package ti4.commands2.map;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.commands2.CommandHelper;
import ti4.commands2.ParentCommand;
import ti4.commands2.Subcommand;
import ti4.helpers.Constants;

public class MapCommand implements ParentCommand {

    private final Map<String, Subcommand> subcommands = Stream.of(
        new AddTile(),
        new AddTileList(),
        new RemoveTile(),
        new AddBorderAnomaly(),
        new RemoveBorderAnomaly(),
        new Preset(),
        new ShowMapSetup(),
        new ShowMapString(),
        new SetMapTemplate()
    ).collect(Collectors.toMap(Subcommand::getName, subcommand -> subcommand));


    @Override
    public String getName() {
        return Constants.MAP;
    }

    @Override
    public String getDescription() {
        return "Game";
    }

    @Override
    public boolean accept(SlashCommandInteractionEvent event) {
        return ParentCommand.super.accept(event) &&
            CommandHelper.acceptIfPlayerInGameAndGameChannel(event);
    }

    @Override
    public Map<String, Subcommand> getSubcommands() {
        return subcommands;
    }
}
