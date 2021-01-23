package com.feed_the_beast.ftbquests.quest.task;

import com.feed_the_beast.ftbquests.FTBQuests;
import com.feed_the_beast.ftbquests.quest.PlayerData;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.mods.ftbguilibrary.config.ConfigFluid;
import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import com.feed_the_beast.mods.ftbguilibrary.config.ConfigNBT;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import me.shedaniel.architectury.fluid.FluidStack;
import me.shedaniel.architectury.registry.Registries;
import me.shedaniel.architectury.utils.Fraction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class FluidTask extends Task
{
	public static final ResourceLocation TANK_TEXTURE = new ResourceLocation(FTBQuests.MOD_ID, "textures/tasks/tank.png");

	public Fluid fluid = Fluids.WATER;
	public CompoundTag fluidNBT = null;
	public long amount = FluidStack.bucketAmount().longValue();

	private FluidStack cachedFluidStack = null;

	public FluidTask(Quest quest)
	{
		super(quest);
	}

	@Override
	public TaskType getType()
	{
		return FTBQuestsTasks.FLUID;
	}

	@Override
	public long getMaxProgress()
	{
		return amount;
	}

	@Override
	public String getMaxProgressString()
	{
		return getVolumeString(amount);
	}

	@Override
	public void writeData(CompoundTag nbt)
	{
		super.writeData(nbt);
		nbt.putString("fluid", Registries.getId(fluid, Registry.FLUID_REGISTRY).toString());
		nbt.putLong("amount", amount);

		if (fluidNBT != null)
		{
			nbt.put("nbt", fluidNBT);
		}
	}

	@Override
	public void readData(CompoundTag nbt)
	{
		super.readData(nbt);

		fluid = Registry.FLUID.get(new ResourceLocation(nbt.getString("fluid")));

		if (fluid == null || fluid == Fluids.EMPTY)
		{
			fluid = Fluids.WATER;
		}

		amount = Math.max(1L, nbt.getLong("amount"));
		fluidNBT = (CompoundTag) nbt.get("nbt");
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer)
	{
		super.writeNetData(buffer);
		buffer.writeResourceLocation(Registries.getId(fluid, Registry.FLUID_REGISTRY));
		buffer.writeNbt(fluidNBT);
		buffer.writeVarLong(amount);
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer)
	{
		super.readNetData(buffer);
		fluid = Registry.FLUID.get(buffer.readResourceLocation());

		if (fluid == null || fluid == Fluids.EMPTY)
		{
			fluid = Fluids.WATER;
		}

		fluidNBT = buffer.readNbt();
		amount = buffer.readVarLong();
	}

	@Override
	public void clearCachedData()
	{
		super.clearCachedData();
		cachedFluidStack = null;
	}

	public FluidStack createFluidStack()
	{
		if (cachedFluidStack == null)
		{
			cachedFluidStack = FluidStack.create(fluid, FluidStack.bucketAmount(), fluidNBT);
		}

		return cachedFluidStack;
	}

	public static String getVolumeString(long a)
	{
		StringBuilder builder = new StringBuilder();

		if (a >= FluidStack.bucketAmount().longValue())
		{
			if (a % FluidStack.bucketAmount().longValue() != 0L)
			{
				builder.append(a / (double) FluidStack.bucketAmount().longValue());
			}
			else
			{
				builder.append(a / FluidStack.bucketAmount().longValue());
			}
		}
		else
		{
			builder.append(a % FluidStack.bucketAmount().longValue());
		}

		builder.append(' ');

		if (a < FluidStack.bucketAmount().longValue())
		{
			builder.append('m');
		}

		builder.append('B');
		return builder.toString();
	}

	@Override
	public Icon getAltIcon()
	{
		FluidStack stack = createFluidStack();
		FluidAttributes a = stack.getFluid().getAttributes();
		return Icon.getIcon(a.getStillTexture(stack).toString()).withTint(Color4I.rgb(a.getColor(stack))).combineWith(Icon.getIcon(FluidTask.TANK_TEXTURE.toString()));
	}

	@Override
	public MutableComponent getAltTitle()
	{
		return new TextComponent(getVolumeString(amount) + " of ").append(createFluidStack().getName());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config)
	{
		super.getConfig(config);

		config.add("fluid", new ConfigFluid(false), fluid, v -> fluid = v, Fluids.WATER);
		config.add("fluid_nbt", new ConfigNBT(), fluidNBT, v -> fluidNBT = v, null);
		config.addLong("amount", amount, v -> amount = v, FluidStack.bucketAmount().longValue(), 1, Long.MAX_VALUE);
	}

	@Override
	public boolean canInsertItem()
	{
		return true;
	}

	@Override
	@Nullable
	public Object getIngredient()
	{
		return createFluidStack();
	}

	@Override
	public TaskData createData(PlayerData data)
	{
		return new Data(this, data);
	}

	public static class Data extends TaskData<FluidTask>
	{
		private Data(FluidTask t, PlayerData data)
		{
			super(t, data);
		}

		@Override
		public String getProgressString()
		{
			return getVolumeString((int) progress);
		}

		public int fill(FluidStack resource, IFluidHandler.FluidAction action)
		{
			if (resource.getAmount() > 0 && !isComplete() && task.createFluidStack().isFluidEqual(resource) && data.canStartTasks(task.quest))
			{
				int add = (int) Math.min(resource.getAmount(), Math.min(Integer.MAX_VALUE, task.amount - progress));

				if (add > 0)
				{
					if (action.execute() && data.file.getSide().isServer())
					{
						addProgress(add);
					}

					return add;
				}
			}

			return 0;
		}
		
		/*
		@Override
		public ItemStack insertItem(ItemStack stack, boolean singleItem, boolean simulate, @Nullable PlayerEntity player)
		{
			if (isComplete())
			{
				return stack;
			}

			IFluidHandlerItem handlerItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

			if (handlerItem == null)
			{
				return stack;
			}

			FluidStack toDrain = task.createFluidStack(Fluid.BUCKET_VOLUME);
			FluidStack drainedFluid = handlerItem.drain(toDrain, false);

			if (drainedFluid == null || drainedFluid.amount <= 0)
			{
				return stack;
			}

			IItemHandler inv = player == null ? null : player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

			if (inv == null)
			{
				inv = ItemDestroyingInventory.INSTANCE;
			}

			ItemStack stack1 = stack.copy();
			FluidActionResult result = FluidUtil.tryFillContainerAndStow(stack1, this, inv, Integer.MAX_VALUE, player, !simulate);

			if (!result.isSuccess())
			{
				result = FluidUtil.tryEmptyContainerAndStow(stack1, this, inv, Integer.MAX_VALUE, player, !simulate);
			}

			if (result.isSuccess())
			{
				return player == null ? ItemStack.EMPTY : result.getResult();
			}

			return stack;
		}

		*/
	}
}