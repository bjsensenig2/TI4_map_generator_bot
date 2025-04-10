package ti4.commands.player;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.GameStateSubcommand;
import ti4.helpers.Constants;
import ti4.helpers.FoWHelper;
import ti4.helpers.StringHelper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;
import ti4.service.info.ListTurnOrderService;

class SCUnpick extends GameStateSubcommand {

    public SCUnpick() {
        super(Constants.SC_UNPICK, "Unpick a Strategy Card", true, true);
        addOptions(new OptionData(OptionType.INTEGER, Constants.STRATEGY_CARD, "Strategy card initiative number").setRequired(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or color returning strategy card").setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getGame();
        boolean isFowPrivateGame = FoWHelper.isPrivateGame(game, event);

        Collection<Player> activePlayers = game.getPlayers().values().stream()
            .filter(player_ -> player_.getFaction() != null && !player_.getFaction().isEmpty() && !"null".equals(player_.getColor()))
            .toList();
        int maxSCsPerPlayer = game.getSCList().size() / activePlayers.size();

        OptionMapping option = event.getOption(Constants.STRATEGY_CARD);
        int scUnpicked = option.getAsInt();

        Player player = getPlayer();
        player.removeSC(scUnpicked);

        int playerSCCount = player.getSCs().size();
        if (playerSCCount >= maxSCsPerPlayer) {
            return;
        }

        String msgExtra = "";
        boolean allPicked = true;
        Player privatePlayer = null;

        boolean nextCorrectPing = false;
        Queue<Player> players = new ArrayDeque<>(activePlayers);
        while (players.iterator().hasNext()) {
            Player player_ = players.poll();
            if (player_ == null || !player_.isRealPlayer()) {
                continue;
            }
            int player_SCCount = player_.getSCs().size();
            if (nextCorrectPing && player_SCCount < maxSCsPerPlayer && player_.getFaction() != null) {
                msgExtra += player_.getRepresentationUnfogged() + " is up to pick their strategy card.";
                privatePlayer = player_;
                allPicked = false;
                break;
            }
            if (player_ == player) {
                nextCorrectPing = true;
            }
            if (player_SCCount < maxSCsPerPlayer && player_.getFaction() != null) {
                players.add(player_);
            }
        }

        //INFORM ALL PLAYER HAVE PICKED
        if (allPicked) {
            msgExtra += "\nAll players picked strategy cards.";

            //ADD A TG TO UNPICKED SC
            game.incrementScTradeGoods();

            Player nextPlayer = game.getActionPhaseTurnOrder().getFirst();

            //INFORM FIRST PLAYER IS UP FOR ACTION
            if (nextPlayer != null) {
                msgExtra += " " + nextPlayer.getRepresentation() + " is up for an action";
                privatePlayer = nextPlayer;
                game.updateActivePlayer(nextPlayer);
            }
        }

        //SEND EXTRA MESSAGE
        if (isFowPrivateGame) {
            if (allPicked) {
                msgExtra = privatePlayer.getRepresentationUnfogged() + ", it is now your turn (your "
                    + StringHelper.ordinal(privatePlayer.getInRoundTurnCount()) + " turn of round " + game.getRound() + ").";
            }
            String fail = "User for next faction not found. Report to ADMIN";
            String success = "The next player has been notified";
            MessageHelper.sendPrivateMessageToPlayer(privatePlayer, game, event, msgExtra, fail, success);
        } else {
            if (allPicked) {
                ListTurnOrderService.turnOrder(event, game);
            }
            MessageHelper.sendMessageToEventChannel(event, msgExtra);
        }
    }
}
