package me.wowkfccc.orchestratorVelocity;

import com.velocitypowered.api.proxy.ProxyServer;
import me.wowkfccc.Ports;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityInventory implements Ports.ServerInventory {
    private final ProxyServer proxy; private final VelocityConfig C;
    public VelocityInventory(ProxyServer proxy, VelocityConfig C){ this.proxy=proxy; this.C=C; }

    @Override public List<String> listServers() {
        return proxy.getAllServers().stream().map(s->s.getServerInfo().getName()).collect(Collectors.toList());
    }
    @Override public double capacityCpu(String serverId){ return C.effectiveCpu(); }
    @Override public double capacityRamMb(String serverId){ return C.effectiveRamMb(); }
}

