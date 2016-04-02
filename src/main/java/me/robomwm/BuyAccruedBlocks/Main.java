package me.robomwm.BuyAccruedBlocks;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;

/**
 * Created by Robo on 4/2/2016.
 */
public class Main extends JavaPlugin implements Listener
{
    GriefPrevention gp = (GriefPrevention)getServer().getPluginManager().getPlugin("GriefPrevention");
    DataStore ds = gp.dataStore;
    RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
    public Economy economy = economyProvider.getProvider();

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage("Not sure why you're trying to use the console to do this");
            return true;
        }
        Player player = (Player)sender;

        if (cmd.getName().equalsIgnoreCase("buy"))
        {
            if (args.length == 0) //If they just did /buy
                return false; //prints usage message as defined in plugin.yml

            int wantsToBuy;

            try
            {
                wantsToBuy = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e) //player didn't enter a number as argument
            {
                return false;
            }

            if (wantsToBuy < 1)
                return false;

            int maxBlocks = gp.config_claims_maxAccruedBlocks;
            PlayerData playerData = ds.getPlayerData(player.getUniqueId());
            int currentBlocks = playerData.getAccruedClaimBlocks();

            if ((wantsToBuy + currentBlocks) > maxBlocks)
            {
                int maximumAllowedBlocks = maxBlocks - currentBlocks;
                sender.sendMessage(ChatColor.RED + "You are trying to buy too many claim blocks. You may buy a maximum of " + maximumAllowedBlocks + " blocks.");
                return true;
            }

            //Economics 101 (is player broke and unable to afford new blocks? nohandout4u)

            double balance = economy.getBalance(player);
            double cost = wantsToBuy * gp.config_economy_claimBlocksPurchaseCost;
            if (cost > balance)
            {
                String broke = MessageFormat.format(Messages.InsufficientFunds.toString(), String.valueOf(cost), String.valueOf(balance));
                sender.sendMessage(ChatColor.RED + broke);
                return true;
            }

            //The transaction is valid, let's take the money and give the blocks
            economy.withdrawPlayer(player, cost);
            playerData.accrueBlocks(currentBlocks + wantsToBuy);
            ds.savePlayerData(player.getUniqueId(), playerData);

            //Hooray a purchase was made
            String hooray = MessageFormat.format(Messages.PurchaseConfirmation.toString(), String.valueOf(cost), String.valueOf(playerData.getRemainingClaimBlocks()));
            player.sendMessage(ChatColor.GREEN + hooray);
            return true;
        }
        return true;
    }
}
