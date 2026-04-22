import qrcode from "qrcode-terminal";

export function renderPairingQr(pairingUrl: string): string {
  if (pairingUrl.trim().length === 0) {
    throw new Error("Pairing URL must not be blank");
  }

  let rendered = "";
  qrcode.generate(pairingUrl, { small: true }, (qr) => {
    rendered = qr;
  });
  return rendered;
}
