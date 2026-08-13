# Contributing

Applies to: 1.9.0 | Last verified: 2026-07-27 | Status: Current

This is a community project maintained by one person. The most valuable contribution is not code.

## Report what works, and what does not

**Device reports are the single most useful thing you can contribute.** Most of the
[compatibility matrix](../compatibility/matrix.md) says Unknown — not because those devices fail,
but because nobody with that combination has said anything.

A useful report names:

1. **The bridge** — make, model, and firmware version. `productName` and `softwareVersionString` on
   the parent device page.
2. **The device** — the exact model.
3. **The driver version** — shown under the links at the top of the Preferences tab.
4. **What happened** — which child devices were created, which driver each got, and what worked or
   did not.

A report without the bridge named cannot be recorded, because the same device behaves differently
behind different bridges.

**Failures are as valuable as successes.** "My Aqara water leak sensor never appeared" is a
documented result; silence is not.

Post reports in the
[community thread](../help/support-and-links.md).

## Report a problem

See [Troubleshooting](../help/troubleshooting.md) first — some behaviour is a documented limitation
rather than a fault. Then check [Known issues](../help/known-issues.md).

If it still looks wrong, post with the information listed under
[asking for help](../help/troubleshooting.md#asking-for-help), including logs captured **while**
reproducing the problem with debug logging on.

Note that defects in Hubitat's own stock drivers are not this project's to fix — see
[Hubitat stock drivers](../drivers/stock-drivers.md).

## Test the BETA

1.9.0 is available as a BETA bundle. Testing it before it becomes the release is genuinely useful,
particularly on bridges and devices the maintainer does not own. See
[Installation](../getting-started/installation.md).

## Contribute code

The project accepts pull requests, and several drivers exist because someone contributed one — the
Door Lock driver, fan control support, and the delayed illuminance handling among them. Contributors
are credited in the [revision history](../project/revisions-history.md).

Work against the `development` branch. Discussing the change in the community thread first tends to
save effort on both sides.

## Contribute documentation

These pages live in
[`docs/user/`](https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/tree/main/docs/user) and
are ordinary Markdown — corrections can be sent as a pull request the same way as code. The
[documentation map](https://github.com/kkossev/Hubitat---Matter-Advanced-Bridge/blob/main/docs/README.md)
explains how the pages are organised and what conventions they follow.

Two rules keep the documentation trustworthy:

- **Every technical claim is checked against the current driver source**, not against the changelog,
  a comment, or an older page.
- **Every compatibility claim carries an evidence label** — see
  [Terminology](terminology.md). Confirmed means tested on a named combination; anything else gets a
  weaker label rather than a confident sentence.

If you would rather not use GitHub, saying what is wrong in the community thread works just as well.

## See also

- [Support and links](../help/support-and-links.md)
- [Terminology](terminology.md)
