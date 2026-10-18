package de.neoscolony.rsaccelerator;

import com.mojang.brigadier.Command;
import com.refinedmods.refinedstorage.common.storage.diskinterface.AbstractDiskInterfaceBlockEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Opt-in, read-only measurement of one loaded native Disk Manipulator for 200 ticks. */
public final class Diagnostics {
 private static final Logger LOG=LoggerFactory.getLogger("NeosRsDiskAccelerator");
 private static Session session;
 private static String last="Noch keine Messung in dieser Serverlaufzeit.";
 private Diagnostics(){}
 private static final class Session {
  final CommandSourceStack source;
  final Object device,node;
  final int startTick;
  final long startNanos=System.nanoTime();
  final Map<String,Long> reasons=new TreeMap<>();
  long hooks,originalMin=Long.MAX_VALUE,originalMax,grantMin=Long.MAX_VALUE,grantMax;
  long attempts,operations,items,nanos,maxNanos,maxItemsPerTick,itemsThisTick;
  int transferTick=Integer.MIN_VALUE,lastSuccessTick=Integer.MIN_VALUE,minGap=Integer.MAX_VALUE,maxGap;
  Session(CommandSourceStack source,Object device,Object node){this.source=source;this.device=device;this.node=node;startTick=source.getServer().getTickCount();}
 }
 public static void register(){
  NeoForge.EVENT_BUS.addListener(Diagnostics::commands);
  NeoForge.EVENT_BUS.addListener(Diagnostics::tick);
  NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->LOG.info("1.0.0 loaded: {}",settings(e.getServer())));
  NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e)->{session=null;last="Noch keine Messung in dieser Serverlaufzeit.";});
 }
 public static String settings(MinecraftServer server){
  return "enabled="+Accelerator.ENABLED.get()+", itemsPerStep="+Accelerator.ITEMS.get()
   +", global="+Accelerator.GLOBAL.get()+", device="+Accelerator.DEVICE.get()+", calls="+Accelerator.CALLS.get()
   +", fallbackAboveMspt="+Accelerator.LOAD_MS.get()+", currentAverageMspt="+String.format(Locale.ROOT,"%.3f",server.getAverageTickTimeNanos()/1e6);
 }
 private static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("neosdisk").requires(s->s.hasPermission(2))
   .then(Commands.literal("accelerator")
    .then(Commands.literal("status").executes(c->{
     c.getSource().sendSuccess(()->Component.literal("Neos RS Disk Accelerator 1.0.0: "+settings(c.getSource().getServer())),false);
     c.getSource().sendSuccess(()->Component.literal(last),false);return Command.SINGLE_SUCCESS;
    }))
    .then(Commands.literal("measure").then(Commands.argument("pos",BlockPosArgument.blockPos())
     .executes(c->begin(c.getSource(),BlockPosArgument.getLoadedBlockPos(c,"pos")))))));
 }
 private static int begin(CommandSourceStack source,BlockPos pos){
  if(session!=null){source.sendFailure(Component.literal("Eine Accelerator-Messung laeuft bereits."));return 0;}
  if(!(source.getLevel().getBlockEntity(pos) instanceof AbstractDiskInterfaceBlockEntity device)){
   source.sendFailure(Component.literal("An dieser Position steht kein originaler RS Disk Manipulator."));return 0;
  }
  var containers=device.getContainerProvider().getContainers();
  if(containers.size()!=1){source.sendFailure(Component.literal("Manipulatorknoten nicht eindeutig verfuegbar."));return 0;}
  boolean hook=Arrays.stream(AbstractDiskInterfaceBlockEntity.class.getDeclaredMethods()).anyMatch(m->m.getName().contains("neos$quota"));
  if(!hook){source.sendFailure(Component.literal("Accelerator-Quotenhook fehlt; keine Messung gestartet."));return 0;}
  session=new Session(source,device,containers.iterator().next().getNode());
  source.sendSuccess(()->Component.literal("Messung fuer 200 natuerliche Serverticks an "+pos.toShortString()+" gestartet. Hook vorhanden. "+settings(source.getServer())+"; Upgrades="+device.getUpgrades()+". Jetzt Transfer laufen lassen."),false);
  return Command.SINGLE_SUCCESS;
 }
 /** Records decisions only for the explicitly selected block. The returned quota is unchanged. */
 public static long quota(Object device,long original,long granted,String reason){
  Session s=session;
  if(s!=null && s.device==device){s.hooks++;s.reasons.merge(reason,1L,Long::sum);s.originalMin=Math.min(s.originalMin,original);s.originalMax=Math.max(s.originalMax,original);s.grantMin=Math.min(s.grantMin,granted);s.grantMax=Math.max(s.grantMax,granted);}
  return granted;
 }
 public static boolean watching(Object node){return session!=null && session.node==node;}
 /** Called after the original transfer has returned; never inserts or extracts any items. */
 public static void transferred(Object node,long items,long nanos){
  Session s=session;if(s==null || s.node!=node)return;
  s.attempts++;if(items>0){s.operations++;s.items+=items;int current=s.source.getServer().getTickCount();if(s.lastSuccessTick!=Integer.MIN_VALUE){int gap=current-s.lastSuccessTick;s.minGap=Math.min(s.minGap,gap);s.maxGap=Math.max(s.maxGap,gap);}s.lastSuccessTick=current;}
  s.nanos+=nanos;s.maxNanos=Math.max(s.maxNanos,nanos);
  int tick=s.source.getServer().getTickCount();if(s.transferTick!=tick){s.transferTick=tick;s.itemsThisTick=0;}
  s.itemsThisTick+=Math.max(0,items);s.maxItemsPerTick=Math.max(s.maxItemsPerTick,s.itemsThisTick);
 }
 private static void tick(ServerTickEvent.Post event){
  Session s=session;if(s==null || event.getServer().getTickCount()-s.startTick<200)return;
  session=null;
  int ticks=event.getServer().getTickCount()-s.startTick;
  double seconds=(System.nanoTime()-s.startNanos)/1e9;
  last=String.format(Locale.ROOT,"Messung: ticks=%d, seconds=%.3f, items=%d, items/s=%.2f, items/tick=%.2f, maxItems/tick=%d, erfolgreicheTransfers=%d, Transfers/s=%.3f, Versuche=%d, nativeTransferTotalMs=%.3f, nativeTransferMaxMs=%.3f; Hookaufrufe=%d, Originalquote=%s, gewaehrteQuote=%s, Gruende=%s",
   ticks,seconds,s.items,s.items/seconds,s.items/(double)ticks,s.maxItemsPerTick,s.operations,s.operations/seconds,s.attempts,s.nanos/1e6,s.maxNanos/1e6,s.hooks,
   s.hooks==0?"keine":s.originalMin+".."+s.originalMax,s.hooks==0?"keine":s.grantMin+".."+s.grantMax,s.reasons);
  last+="; TransferabstandTicks="+(s.minGap==Integer.MAX_VALUE?"nicht messbar":s.minGap+".."+s.maxGap);
  String message=last;
  s.source.sendSuccess(()->Component.literal(message),false);LOG.info("{}",message);
 }
}