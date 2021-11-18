import { WebPlugin } from '@capacitor/core';

import type { RFIDPlugin } from './definitions';

export class RFIDWeb extends WebPlugin implements RFIDPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
