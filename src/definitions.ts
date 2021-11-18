export interface RFIDPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
