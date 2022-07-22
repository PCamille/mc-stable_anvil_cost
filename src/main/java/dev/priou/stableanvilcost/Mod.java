package dev.priou.stableanvilcost;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@net.minecraftforge.fml.common.Mod(Mod.MODID)
public class Mod
{
    public static final String MODID = "stable_anvil_cost";
    public Mod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::load);
    }

    private void load(final FMLLoadCompleteEvent e) {
        MinecraftForge.EVENT_BUS.register(new AnvilEvent());
    }
}
