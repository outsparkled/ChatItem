package me.dadus33.chatitem.chatmanager.v1.listeners;

import static me.dadus33.chatitem.chatmanager.ChatManager.SEPARATOR;

import java.util.HashMap;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.chatmanager.v1.PacketEditingChatManager;
import me.dadus33.chatitem.utils.Storage;

public class ChatEventListener implements Listener {

	private final static String LEFT = "{remaining}";
	private final HashMap<String, Long> COOLDOWNS = new HashMap<>();
	private final PacketEditingChatManager manage;

	public ChatEventListener(PacketEditingChatManager manage) {
		this.manage = manage;
	}

	public Storage getStorage() {
		return this.manage.getStorage();
	}

	private String calculateTime(long seconds) {
		if (seconds < 60) {
			return seconds + getStorage().SECONDS;
		}
		if (seconds < 3600) {
			StringBuilder builder = new StringBuilder();
			int minutes = (int) seconds / 60;
			builder.append(minutes).append(getStorage().MINUTES);
			int secs = (int) seconds - minutes * 60;
			if (secs != 0) {
				builder.append(" ").append(secs).append(getStorage().SECONDS);
			}
			return builder.toString();
		}
		StringBuilder builder = new StringBuilder();
		int hours = (int) seconds / 3600;
		builder.append(hours).append(getStorage().HOURS);
		int minutes = (int) (seconds / 60) - (hours * 60);
		if (minutes != 0) {
			builder.append(" ").append(minutes).append(getStorage().MINUTES);
		}
		int secs = (int) (seconds - ((seconds / 60) * 60));
		if (secs != 0) {
			builder.append(" ").append(secs).append(getStorage().SECONDS);
		}
		return builder.toString();
	}

	private int countOccurrences(String findStr, String str) {
		int lastIndex = 0;
		int count = 0;
		while (lastIndex != -1) {

			lastIndex = str.indexOf(findStr, lastIndex);

			if (lastIndex != -1) {
				count++;
				lastIndex += findStr.length();
			}
		}
		return count;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST) // We need to have lowest priority in order
																			// to get to the event before DeluxeChat or
																			// other plugins do
	public void onChat(AsyncPlayerChatEvent e) {
		ChatItem.debug("(v1) Check for v1 system. Cancelled: " + e.isCancelled());
		if (e.getMessage().indexOf(SEPARATOR) != -1) { // If the BELL character is found, we have to remove it
			String msg = e.getMessage().replace(Character.toString(SEPARATOR), "");
			ChatItem.debug("Already bell in message: " + e.getMessage());
			e.setMessage(msg);
		}
		boolean found = false;
		final String oldMsg = e.getMessage();
		for (String rep : getStorage().PLACEHOLDERS)
			if (oldMsg.contains(rep)) {
				found = true;
				break;
			}

		if (!found) {
			ChatItem.debug("(v1) not found placeholders in: " + e.getMessage());
			return;
		}

		Player p = e.getPlayer();

		if (getStorage().PERMISSION_ENABLED && !p.hasPermission(getStorage().PERMISSION_NAME)) {
			if (!getStorage().LET_MESSAGE_THROUGH) {
				e.setCancelled(true);
			}
			if (!getStorage().NO_PERMISSION_MESSAGE.isEmpty() && getStorage().SHOW_NO_PERM_NORMAL) {
				p.sendMessage(getStorage().NO_PERMISSION_MESSAGE);
			}
			return;
		}
		if (p.getItemInHand().getType().equals(Material.AIR)) {
			if (getStorage().DENY_IF_NO_ITEM) {
				e.setCancelled(true);
				if (!getStorage().DENY_MESSAGE.isEmpty())
					e.getPlayer().sendMessage(getStorage().DENY_MESSAGE);
				return;
			}
			if (getStorage().HAND_DISABLED) {
				return;
			}
		}
		if (getStorage().COOLDOWN > 0 && !p.hasPermission("chatitem.ignore-cooldown")) {
			if (COOLDOWNS.containsKey(p.getName())) {
				long start = COOLDOWNS.get(p.getName());
				long current = System.currentTimeMillis() / 1000;
				long elapsed = current - start;
				if (elapsed >= getStorage().COOLDOWN) {
					COOLDOWNS.remove(p.getName());
				} else {
					if (!getStorage().LET_MESSAGE_THROUGH) {
						e.setCancelled(true);
					}
					if (!getStorage().COOLDOWN_MESSAGE.isEmpty()) {
						long left = (start + getStorage().COOLDOWN) - current;
						p.sendMessage(getStorage().COOLDOWN_MESSAGE.replace(LEFT, calculateTime(left)));
					}
					ChatItem.debug("(v1) Cooldown");
					return;
				}
			}
		}
		String s = e.getMessage(), firstPlaceholder = getStorage().PLACEHOLDERS.get(0);
		for (String placeholder : getStorage().PLACEHOLDERS) {
			s = s.replace(placeholder, firstPlaceholder);
		}
		int occurrences = countOccurrences(firstPlaceholder, s);

		if (occurrences > getStorage().LIMIT) {
			e.setCancelled(true);
			if (getStorage().LIMIT_MESSAGE.isEmpty()) {
				return;
			}
			p.sendMessage(getStorage().LIMIT_MESSAGE);
			return;
		}

		ChatItem.debug("(v1) Set placeholder: " + e.getMessage());
		try {
			StringJoiner msg = new StringJoiner(" ");
			for(String part : s.split(" ")) {
				if(part.equalsIgnoreCase(firstPlaceholder)) {
					msg.add(firstPlaceholder + SEPARATOR + p.getName());
				} else {
					msg.add(part);
				}
			}
			e.setMessage(msg.toString());
			e.setFormat(e.getFormat().replace(oldMsg, e.getMessage())); // set own message for plugin that already put the message into the format
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		if (!p.hasPermission("chatitem.ignore-cooldown")) {
			COOLDOWNS.put(p.getName(), System.currentTimeMillis() / 1000);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(final PlayerCommandPreprocessEvent e) {
		if (e.getMessage().indexOf(SEPARATOR) != -1) { // If the BELL character is found, we have to remove it
			String msg = e.getMessage().replace(Character.toString(SEPARATOR), "");
			e.setMessage(msg);
		}
		String commandString = e.getMessage().split(" ")[0].replaceAll("^/+", ""); // First part of the command, without
																					// leading slashes and without
																					// arguments
		Command cmd = Bukkit.getPluginCommand(commandString);
		if (cmd == null) { // not a plugin command
			if (!getStorage().ALLOWED_DEFAULT_COMMANDS.contains(commandString)) {
				return;
			}
		} else {
			if (!getStorage().ALLOWED_PLUGIN_COMMANDS.contains(cmd)) {
				return;
			}
		}

		Player p = e.getPlayer();
		boolean found = false;

		for (String rep : getStorage().PLACEHOLDERS) {
			if (e.getMessage().contains(rep)) {
				found = true;
				break;
			}
		}

		if (!found) {
			return;
		}

		if (!p.hasPermission("chatitem.use")) {
			if (!getStorage().NO_PERMISSION_MESSAGE.isEmpty() && getStorage().SHOW_NO_PERM_COMMAND) {
				p.sendMessage(getStorage().NO_PERMISSION_MESSAGE);
			}
			e.setCancelled(true);
			return;
		}

		if (e.getPlayer().getItemInHand().getType().equals(Material.AIR)) {
			if (getStorage().DENY_IF_NO_ITEM) {
				e.setCancelled(true);
				if (!getStorage().DENY_MESSAGE.isEmpty()) {
					e.getPlayer().sendMessage(getStorage().DENY_MESSAGE);
				}
			}
			if (getStorage().HAND_DISABLED) {
				return;
			}
		}

		if (getStorage().COOLDOWN > 0 && !p.hasPermission("chatitem.ignore-cooldown")) {
			if (COOLDOWNS.containsKey(p.getName())) {
				long start = COOLDOWNS.get(p.getName());
				long current = System.currentTimeMillis() / 1000;
				long elapsed = current - start;
				if (elapsed >= getStorage().COOLDOWN) {
					COOLDOWNS.remove(p.getName());
				} else {
					if (!getStorage().LET_MESSAGE_THROUGH) {
						e.setCancelled(true);
					}
					if (!getStorage().COOLDOWN_MESSAGE.isEmpty()) {
						long left = (start + getStorage().COOLDOWN) - current;
						p.sendMessage(getStorage().COOLDOWN_MESSAGE.replace(LEFT, calculateTime(left)));
					}
					return;
				}
			}
		}

		String s = e.getMessage();
		for (String placeholder : getStorage().PLACEHOLDERS) {
			s = s.replace(placeholder, getStorage().PLACEHOLDERS.get(0));
		}
		int occurrences = countOccurrences(getStorage().PLACEHOLDERS.get(0), s);

		if (occurrences > getStorage().LIMIT) {
			e.setCancelled(true);
			if (getStorage().LIMIT_MESSAGE.isEmpty()) {
				return;
			}
			e.getPlayer().sendMessage(getStorage().LIMIT_MESSAGE);
			return;
		}

		StringBuilder sb = new StringBuilder(e.getMessage());
		sb.append(SEPARATOR).append(e.getPlayer().getName());
		e.setMessage(sb.toString());
		if (!p.hasPermission("chatitem.ignore-cooldown")) {
			COOLDOWNS.put(p.getName(), System.currentTimeMillis() / 1000);
		}
	}
}
