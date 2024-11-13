package ti4.commands.cardsac;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.commands.GameStateSubcommand;
import ti4.helpers.ActionCardHelper;
import ti4.helpers.Constants;
import ti4.message.MessageHelper;

class ACInfo extends GameStateSubcommand {

    public ACInfo() {
        super(Constants.INFO, "Send Action Cards to your Cards Info thread", false, true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var game = getGame();
        var player = getPlayer();
        ActionCardHelper.sendActionCardInfo(game, player, event);
        MessageHelper.sendMessageToEventChannel(event, "AC Info Sent");
    }
}
