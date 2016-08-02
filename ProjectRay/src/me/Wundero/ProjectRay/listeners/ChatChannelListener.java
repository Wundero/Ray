package me.Wundero.ProjectRay.listeners;
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

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import me.Wundero.ProjectRay.Ray;
import me.Wundero.ProjectRay.framework.channel.ChatChannel;

public class ChatChannelListener {

	@Listener(order = Order.EARLY)
	public void onJoin(ClientConnectionEvent.Join event) {
		ChatChannel mostIn = null;
		for (ChatChannel c : Ray.get().getChannels().getJoinableChannels(event.getTargetEntity(), true)) {
			if (c.isAutojoin()) {
				c.addMember(event.getTargetEntity());
				if (mostIn == null) {
					mostIn = c;
				} else {
					if (mostIn.compareTo(c) < 0) {
						mostIn = c;
					}
				}
			}
		}
		if (mostIn != null) {
			event.getTargetEntity().setMessageChannel(mostIn);
		}
	}

}
