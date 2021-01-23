package com.feed_the_beast.ftbquests.net;

import com.feed_the_beast.ftbquests.quest.PlayerData;
import com.feed_the_beast.ftbquests.quest.ServerQuestFile;
import com.feed_the_beast.ftbquests.quest.task.Task;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class MessageSubmitTask extends MessageBase
{
	private final int task;

	MessageSubmitTask(FriendlyByteBuf buffer)
	{
		task = buffer.readVarInt();
	}

	public MessageSubmitTask(int t)
	{
		task = t;
	}

	@Override
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeVarInt(task);
	}

	@Override
	public void handle(NetworkManager.PacketContext context)
	{
		ServerPlayer player = (ServerPlayer) context.getPlayer();
		PlayerData data = PlayerData.get(player);
		Task t = ServerQuestFile.INSTANCE.getTask(task);

		if (t != null && data.canStartTasks(t.quest))
		{
			data.getTaskData(t).submitTask(player);
		}
	}
}