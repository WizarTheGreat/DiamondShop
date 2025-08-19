package plugin.diamondshop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("diamondshop")) {
            if (args.length == 0) {
                sender.sendMessage("Incorrect formatting");
                return true;
            }
            if (args[0].toString().equalsIgnoreCase("help")) {
                Player player = (Player) sender;
                player.sendMessage("In order make a shop, put this on a sign on a chest");
                player.sendMessage("(Shop)");
                player.sendMessage("(number) (Diamond/Diamonds)");
                player.sendMessage("(number) (item name)");
                player.sendMessage("(your name)");
                return true;
            }
        }
        return true;
    }
}
