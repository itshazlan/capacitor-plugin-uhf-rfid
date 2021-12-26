import { PluginListenerHandle } from "@capacitor/core";

export interface RFIDPlugin {

  /**
   * Connect handheld to the module.
   *
   * @since 1.0.0
   */
  connect(options: ReaderConnectOptions): Promise<void>;

  /**
   * Disconnect handheld to the module.
   *
   * @since 1.0.0
   */
  disconnect(): Promise<void>;

  /**
   * Begin start inventory task (deprecated).
   *
   * @since 1.0.0
   */
  startScan(): Promise<void>;

  /**
   * Stop inventory task (deprecated).
   *
   * @since 1.0.0
   */
  stopScan(): Promise<void>;

  /**
   * Clear scanned inventory task.
   *
   * @since 1.0.0
   */
  clear(): Promise<void>;

  /**
   * Set options for handheld using recommended settings (Antenna power, Mode set).
   *
   * @since 1.0.0
   */
  setOption(options: ReaderOptions): Promise<void>;

  addWatcher(): Promise<string>;

  removeWatcher(): Promise<void>;

  /**
   * Called when an action is performed.
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'rfid',
    listenerFunc: (data: any) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove all native listeners for this plugin.
   *
   * @since 1.0.0
   */
  removeAllListeners(): Promise<void>;
};

export interface ReaderScanResult {
  tags: TagList[];
}

export interface TagList {
  sort: string;
  epcId: string;
  count: string;
  ant: string;
  protocol: string;
  rssi: string;
  frequency: string;
  rfu: string;
  timestamp: string;
  emd: string;
  nxpu8_tid: string;
  nxpu8_bid: string;
}

export interface ReaderConnectOptions {
  address: string;
}

export interface ReaderConnectResult {
  connected: boolean;
}

export interface ReaderOptions {
  readerMode: string;
  antennaPower: number;
}
