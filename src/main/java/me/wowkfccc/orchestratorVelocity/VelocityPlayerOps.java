package me.wowkfccc.orchestratorVelocity;

import com.velocitypowered.api.proxy.ProxyServer;
import me.wowkfccc.Ports;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VelocityPlayerOps implements Ports.PlayerOps {
    private final ProxyServer proxy; private final VelocityConfig C;
    public VelocityPlayerOps(ProxyServer proxy, VelocityConfig C){ this.proxy=proxy; this.C=C; }

    @Override public List<Player> listOnlinePlayers() {
        return proxy.getAllPlayers().stream().map(p ->
                new Player(p.getUniqueId().toString(),
                        p.getUsername(),
                        p.getCurrentServer().map(s->s.getServerInfo().getName()).orElse("unknown"),
                        "AFK"
                )).collect(Collectors.toList());
    }

    @Override public void movePlayer(String playerId, String targetServerId) {
        proxy.getPlayer(UUID.fromString(playerId)).ifPresent(p ->
                proxy.getServer(targetServerId).ifPresent(dst -> p.createConnectionRequest(dst).fireAndForget())
        );
    }
}

