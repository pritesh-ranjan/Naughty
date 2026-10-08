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

* **Certificate Owner**: `CN=Naughty FOSS, OU=Release, O=Naughty Open Source, L=Local, ST=State, C=US`
* **Algorithm**: RSA 2048-bit with SHA-384
* **Valid Through**: February 22, 2054
* **SHA-256 Fingerprint**:  
  `7A:1C:0C:93:A9:93:D8:93:48:67:5C:C3:78:C7:57:8A:6C:BA:12:30:E7:2B:93:19:71:74:F4:66:36:7D:E9:FA`
* **SHA-1 Fingerprint**:  
  `E5:2B:EE:15:43:A5:A5:95:AF:8D:91:41:E6:75:04:CB:40:E3:81:EA`

### How to Verify an APK Before Installation

Run `apksigner` (included in the Android SDK build-tools):

```bash
apksigner verify --verbose --print-certs Naughty-v*-release.apk
```

**Verify that:**
1. `Verified using v2 scheme (APK Signature Scheme v2): true`
2. `Verified using v3 scheme (APK Signature Scheme v3): true`
3. The SHA-256 digest exactly matches:  
   `7a1c0c93a993d89348675cc378c7578a6cba1230e72b93197174f466367de9fa`

---

## 4. Architectural Security Safeguards

* **No Network Socket**: `android.permission.INTERNET` is explicitly stripped at compile time (`tools:node="remove"`). The OS blocks the app from creating any TCP/UDP sockets.
* **Biometric Session Isolation**: Session state is retained in-memory via `NoteLockSession` and cleared upon application termination.
* **Automatic Cloud Exclusion**: System cloud backups are disabled via `data_extraction_rules.xml` to prevent unencrypted cloud syncing of local notes.
