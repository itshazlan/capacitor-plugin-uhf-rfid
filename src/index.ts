import { registerPlugin } from '@capacitor/core';

import type { RFIDPlugin } from './definitions';

const RFID = registerPlugin<RFIDPlugin>('RFID', {
  web: () => import('./web').then(m => new m.RFIDWeb()),
});

export * from './definitions';
export { RFID };
