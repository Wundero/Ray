package me.Wundero.Ray.commands.channel;
/*
 The MIT License (MIT)

 Copyright (c) 2016 Wundero

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import me.Wundero.Ray.Ray;
import me.Wundero.Ray.framework.channel.ChatChannel;
import me.Wundero.Ray.framework.channel.Role;
import me.Wundero.Ray.framework.player.RayPlayer;
import me.Wundero.Ray.utils.Utils;

/**
 * A command to ban a user from a channel.
 */
public class ChannelBanCommand implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!(src.hasPermission("ray.channel.ban"))) {
			throw new CommandException(Text.of(TextColors.RED, "You are not allowed to do that!"));
		}
		if (!Ray.get().getChannels().useChannels()) {
			throw new CommandException(Text.of(TextColors.RED, "Channels are disabled!"));
		}
		if (src instanceof Player) {
			Player p = (Player) src;
			try {

				Role r = RayPlayer.get(p).getActiveChannel().getRole(p);
				if (r != Role.MOD) {
					throw new CommandException(Text.of(TextColors.RED, "You are not allowed to do that!"));
				}
			} catch (NullPointerException e) {
				throw new CommandException(Text.of(TextColors.RED, "Channels are disabled!"));
			}
		}
		Player p = (Player) args.getOne("target").get();
		Optional<String> ch = args.getOne("channel");
		if (ch.isPresent()) {
			String channel = ch.get();
			ChatChannel cha = Ray.get().getChannels().getChannel(channel, true);
			if (cha == null) {
				throw new CommandException(Text.of(TextColors.RED, "That channel does not exist!"));
			}
			try {
				if (!cha.hasMember(p)) {
					throw new CommandException(
							Text.of(TextColors.RED, "Player " + p.getName() + " is not a member of " + channel + "!"));
				}
			} catch (NullPointerException e) {
				throw new CommandException(
						Text.of(TextColors.RED, "Player " + p.getName() + " is not a member of " + channel + "!"));
			}
		} else {
			try {
				if (!RayPlayer.get(p).getActiveChannel().hasMember(p)) {
					throw new CommandException(Text.of(TextColors.RED, "Player " + p.getName() + " is not a member of "
							+ RayPlayer.get(p).getActiveChannel().getName() + "!"));
				}
			} catch (NullPointerException e) {
				throw new CommandException(Text.of(TextColors.RED, "Channels are disabled!"));
			}
		}
		ChatChannel cc = ch.isPresent()
				? Utils.wrap(Ray.get().getChannels().getChannel(ch.get())).orElse(RayPlayer.get(p).getActiveChannel())
				: RayPlayer.get(p).getActiveChannel();
		cc.toggleBan(p);
		boolean cur = cc.isBanned(p);
		String s = "no" + (cur ? "w" : " longer");
		String x = " in channel " + (ch.isPresent() ? ch.get() : RayPlayer.get(p).getActiveChannel().getName());
		src.sendMessage(Text.of(TextColors.RED, "" + p.getName() + " is " + s + " banned" + x + "!"));
		return CommandResult.success();
	}

}
