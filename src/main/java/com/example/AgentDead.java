package com.example;

final class AgentDead implements AgentEvent {
    final Agent killer;
    final Agent dead;
    AgentDead(Agent killer, Agent dead) { this.killer = killer; this.dead = dead; }
}
