package com.shahmdmahi.hpctv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WaitingRoomSnapshot(
    val clinic: ClinicInfo = ClinicInfo(),
    val activeCalls: List<CallAnnouncement> = emptyList(),
    val consultationQueue: ConsultationQueueData = ConsultationQueueData(),
    val therapyQueue: TherapyQueueData = TherapyQueueData(),
    val bulletinNotices: List<BulletinNoticeItem> = emptyList(),
    val telemetry: TelemetryInfo = TelemetryInfo()
)

@Serializable
data class ClinicInfo(
    val name: String = "HEALTH & PAIN CARE CENTER",
    val nameBangla: String = "হেলথ অ্যান্ড পেইন কেয়ার সেন্টার",
    val branch: String = "Chanchra Dalmill, Sadar, Jashore",
    val addressBangla: String = "চাঁচড়া ডালমিল (পুলিশ ফাঁড়ির বিপরীতে), সদর, যশোর-৭৪০০",
    val phone: String = "01822-000035",
    val hotline: String = "01822-000035",
    val logoUrl: String = "/icon.png",
    val serverTime: String = ""
)

@Serializable
data class CallAnnouncement(
    val appointmentId: String = "",
    val serialNumber: Int? = null,
    val patientName: String = "",
    val patientMrn: String? = null,
    val gender: String? = null,
    val roomNumber: String = "",
    val roomPurpose: String = "Doctor Consultation",
    val station: String = "CONSULTATION_ROOM",
    val calledAt: String = "",
    val sound: String = "chime",
    val voiceBn: String = "",
    val voiceEn: String = ""
)

@Serializable
data class ConsultationQueueData(
    val doctors: List<DoctorChamberQueue> = emptyList(),
    val currentlyServing: List<CurrentlyServingPatient> = emptyList(),
    val waitingQueue: List<WaitingQueuePatient> = emptyList(),
    val totalWaiting: Int = 0
)

@Serializable
data class DoctorChamberQueue(
    val doctorId: String = "",
    val doctorName: String = "",
    val roomNumber: String = "",
    val roomPurpose: String = "Doctor Consultation",
    val currentlyServing: ServingStatus? = null,
    val waitingCount: Int = 0,
    val waitingSerials: List<WaitingSerialItem> = emptyList()
)

@Serializable
data class ServingStatus(
    val appointmentId: String? = null,
    val serialNumber: Int? = null,
    val patientName: String = "",
    val status: String = "",
    val startedAt: String? = null
)

@Serializable
data class WaitingSerialItem(
    val appointmentId: String? = null,
    val serialNumber: Int? = null,
    val patientName: String = "",
    val checkInTime: String? = null,
    val toldTime: String? = null
)

@Serializable
data class CurrentlyServingPatient(
    val doctorId: String = "",
    val doctorName: String = "",
    val roomNumber: String = "",
    val serialNumber: Int? = null,
    val patientName: String = ""
)

@Serializable
data class WaitingQueuePatient(
    val appointmentId: String = "",
    val serialNumber: Int? = null,
    val patientName: String = "",
    val doctorName: String? = null,
    val checkInTime: String? = null
)

@Serializable
data class TherapyQueueData(
    val rooms: List<TherapyRoomState> = emptyList(),
    val activeTherapyCount: Int = 0,
    val waitingTherapyCount: Int = 0,
    val estimatedWaitMinutes: Int = 0,
    val waitingQueue: List<WaitingSerialItem> = emptyList()
)

@Serializable
data class TherapyRoomState(
    val roomId: String = "",
    val roomNumber: String = "",
    val purpose: String = "Therapy Room",
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, MAINTENANCE
    val currentPatient: CurrentTherapyPatient? = null,
    val timer: TherapyTimerInfo? = null
)

@Serializable
data class CurrentTherapyPatient(
    val appointmentId: String = "",
    val serialNumber: Int? = null,
    val patientName: String = "",
    val startedAt: String? = null
)

@Serializable
data class TherapyTimerInfo(
    val isRunning: Boolean = false,
    val currentStepIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val modalityName: String = "",
    val isAllCompleted: Boolean = false
)

@Serializable
data class BulletinNoticeItem(
    val id: String = "",
    val title: String? = null,
    val message: String = "",
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val targetAudience: String = "ALL",
    val createdAt: String = ""
)

@Serializable
data class TelemetryInfo(
    val version: String = "1.0",
    val mode: String = "production",
    val sseStreamUrl: String = "/api/v1/tv/waiting-room/stream",
    val serverTimestamp: Long = 0L
)

@Serializable
data class DiscoveredServer(
    val service: String = "",
    val ip: String = "",
    val port: Int = 3000,
    val ssl: Boolean = true,
    val name: String = "Health & Pain Care Center"
) {
    val baseUrl: String
        get() = "${if (ssl) "https" else "http"}://$ip:$port"
}

@Serializable
data class SystemBroadcastAlert(
    val message: String = "",
    val priority: String = "HIGH",
    val sentBy: String? = null,
    val sentAt: String? = null
)

@Serializable
data class HeartbeatPayload(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String = "ANDROID_TV",
    val role: String = "WAITING_ROOM",
    val station: String = "Waiting Room TV",
    val latencyMs: Int = 0,
    val clientTimestamp: Long = 0L,
    val browser: String = "AndroidTV/Native",
    val userAgent: String = "HPC-AndroidTV/1.0"
)

@Serializable
data class HeartbeatResponse(
    val status: String = "ok",
    val serverTimestamp: Long = 0L,
    val receivedIp: String? = null,
    val ok: Boolean = true
)
