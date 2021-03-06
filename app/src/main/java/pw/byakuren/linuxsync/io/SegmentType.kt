package pw.byakuren.linuxsync.io

enum class SegmentType(val header: Byte) {
    Title(0x01),
    Body(0x02),
    Image(0x03),
    AppName(0x04),
    NotificationId(0x05),
    Action(0x08),
    End(0x7F),
}