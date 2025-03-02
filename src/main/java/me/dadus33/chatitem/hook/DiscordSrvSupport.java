package me.dadus33.chatitem.hook;

import org.bukkit.entity.Player;

import github.scarsz.discordsrv.DiscordSRV;

public class DiscordSrvSupport {

	public static void sendchatMessage(Player p, String message) {
		DiscordSRV pl = DiscordSRV.getPlugin();
		if (DiscordSRV.config().getBooleanElse("UseModernPaperChatEvent", false) && pl.isModernChatEventAvailable()) {
			return;
		}
		pl.processChatMessage(p, message, pl.getOptionalChannel("global"), false);
	}
}
