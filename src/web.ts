import { WebPlugin } from '@capacitor/core';
import { ReaderConnectOptions, ReaderOptions } from '.';
import { OptionsRequiredError } from './utils/errors';

import type { RFIDPlugin } from './definitions';

export class RFIDWeb extends WebPlugin implements RFIDPlugin {

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async connect(options: ReaderConnectOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error("Method not implemented.");
  }

  async disconnect(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async setOption(options: ReaderOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error("Method not implemented.");
  }

  async startScan(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async stopScan(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async clear(): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async addWatcher(): Promise<string> {
    throw new Error("Method not implemented.");
  }
  
  async removeWatcher(): Promise<void> {
    throw new Error("Method not implemented.");
  }
}
