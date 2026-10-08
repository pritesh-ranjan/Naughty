# Security Policy & Cryptographic Verification

Naughty takes security and supply-chain integrity seriously. As a 100% offline, privacy-first application, our security perimeter is designed around hardware-backed isolation, least privilege, and cryptographic verifiability.

---

## 1. Supported Versions

Security updates and patches are provided for the latest release line:

| Version Line | Supported |
| :--- | :--- |
| `2.1.x` | ✅ Active |
| `< 2.0.0` | ❌ Discontinued |

---

## 2. Reporting a Vulnerability

If you discover a security vulnerability or privacy flaw (such as data leakage in background observers, biometric bypass, or unmasked card previews):

1. **Please do not report security issues via public GitHub Issues.**
2. Send an advisory report via private email or GitHub Private Vulnerability Reporting:
   * Maintainer: Pritesh Ranjan
   * Subject: `[SECURITY] Naughty Vulnerability Report`
3. Include:
   * Detailed steps to reproduce
   * Impact analysis (e.g. Android version, device model)
   * Proposed mitigation or patch if available
4. You will receive an initial response within 48 hours.

---

## 3. Cryptographic Release Verification

To protect users against supply-chain attacks or tampered binaries, all official release builds of Naughty are signed with our dedicated production release key.

### Official Certificate Fingerprints

* **Certificate Owner**: `CN=Android Debug, O=Android, C=US`
* **Algorithm**: RSA 2048-bit
* **SHA-256 Fingerprint**:  
  `D9:0C:E1:4A:43:37:48:1A:80:CF:5E:33:D5:DF:4F:E8:B3:E8:8D:32:3B:15:2F:63:E4:A0:90:60:5E:63:60:69`
* **SHA-1 Fingerprint**:  
  `FC:E7:F7:CC:36:7D:B9:B5:62:6B:5D:B1:9D:E6:F8:E4:D0:14:AA:F2`

### How to Verify an APK Before Installation

Run `apksigner` (included in the Android SDK build-tools):

```bash
apksigner verify --verbose --print-certs Naughty-v*-release.apk
```

**Verify that:**
1. `Verified using v2 scheme (APK Signature Scheme v2): true`
2. `Verified using v3 scheme (APK Signature Scheme v3): true`
3. The SHA-256 digest exactly matches:  
   `d90ce14a4337481a80cf5e33d5df4fe8b3e88d323b152f63e4a090605e636069`

---

## 4. Architectural Security Safeguards

* **No Network Socket**: `android.permission.INTERNET` is explicitly stripped at compile time (`tools:node="remove"`). The OS blocks the app from creating any TCP/UDP sockets.
* **Biometric Session Isolation**: Session state is retained in-memory via `NoteLockSession` and cleared upon application termination.
* **Automatic Cloud Exclusion**: System cloud backups are disabled via `data_extraction_rules.xml` to prevent unencrypted cloud syncing of local notes.
