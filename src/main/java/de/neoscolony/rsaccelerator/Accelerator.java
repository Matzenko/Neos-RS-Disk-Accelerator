package de.neoscolony.rsaccelerator;

import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.IdentityHashMap;

@Mod(Accelerator.ID)
public final class Accelerator {
 public static final String ID="neos_rs_disk_accelerator";
 public static final ModConfigSpec SPEC;
 public static final ModConfigSpec.BooleanValue ENABLED;
 public static final ModConfigSpec.IntValue ITEMS,GLOBAL,DEVICE,CALLS;
 public static final ModConfigSpec.DoubleValue LOAD_MS;
 static {
  var b=new ModConfigSpec.Builder();
  ENABLED=b.comment("false restores original RS transfer quotas.").define("enabled",true);
  ITEMS=b.comment("Items per native work step WITH stack upgrade; without it 1/64. Intervals unchanged.").defineInRange("itemsPerStep",8192,64,16384);
  GLOBAL=b.defineInRange("maxItemsPerServerTick",65536,64,1048576);
  DEVICE=b.defineInRange("maxItemsPerDeviceTick",16384,64,1048576);
  CALLS=b.comment("Maximum accelerated quota grants per server tick; no extra work ticks are invoked.").defineInRange("maxWorkStepsPerServerTick",256,1,4096);
  LOAD_MS=b.comment("Above this average MSPT, fall back to original RS quota.").defineInRange("fallbackAboveMspt",45.0,1.0,1000.0);
  SPEC=b.build();
 }
 private static net.minecraft.server.MinecraftServer owner;
 private static int tick=Integer.MIN_VALUE,calls;
 private static long remaining;
 private static final IdentityHashMap<Object,Long> used=new IdentityHashMap<>();
 public Accelerator(ModContainer container){Diagnostics.register();container.registerConfig(ModConfig.Type.SERVER,SPEC);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->{owner=null;used.clear();tick=Integer.MIN_VALUE;});}
 public static long quota(Object device,Storage storage,long original){
  if(!ENABLED.get())return Diagnostics.quota(device,original,original,"disabled");
  if(!(storage instanceof SerializableStorage s)
     || s.getType()!=RefinedStorageApi.INSTANCE.getItemStorageType() || original!=1 && original!=64)return Diagnostics.quota(device,original,original,"unsupported_storage_or_quota");
  var server=ServerLifecycleHooks.getCurrentServer();
  if(server==null || !server.isSameThread())return Diagnostics.quota(device,original,original,"no_server_or_wrong_thread");
  if(server.getAverageTickTimeNanos()>LOAD_MS.get()*1_000_000.0)return Diagnostics.quota(device,original,original,"mspt_fallback");
  if(owner!=server || tick!=server.getTickCount()){
   owner=server;tick=server.getTickCount();remaining=GLOBAL.get();calls=0;used.clear();
  }
  if(calls>=CALLS.get())return Diagnostics.quota(device,original,0,"call_budget");
  long desired=original==64?ITEMS.get():Math.max(1,ITEMS.get()/64);
  long prior=used.getOrDefault(device,0L);
  long grant=Math.max(0,Math.min(desired,Math.min(remaining,DEVICE.get()-prior)));
  if(grant>0){remaining-=grant;used.put(device,prior+grant);calls++;}
  return Diagnostics.quota(device,original,grant,grant==desired?"accelerated":grant==0?"item_budget_exhausted":"item_budget_limited");
 }
}