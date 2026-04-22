declare module "qrcode-terminal" {
  type QrOptions = {
    small?: boolean;
  };

  const qrcode: {
    generate(input: string, options: QrOptions, callback: (qr: string) => void): void;
  };

  export default qrcode;
}
