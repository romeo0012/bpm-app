package com.camunda.plugin;

import org.camunda.bpm.cockpit.plugin.spi.impl.AbstractCockpitPlugin;

public class HistoryPlugin extends AbstractCockpitPlugin {

  public static final String ID = "history";

  @Override
  public String getId() {
    return ID;
  }
}