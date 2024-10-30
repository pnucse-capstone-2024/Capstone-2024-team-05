import org.json.JSONObject

data class ViolationData(
    val afosFid: Long,
    val afosId: Long,
    val violationType: String,
    val bjdCode: String,
    val spotCode: String,
    val sidoSggName: String,
    val spotName: String,
    val occrrncCnt: Int,
    val casltCnt: Int,
    val dthDnvCnt: Int,
    val seDnvCnt: Int,
    val slDnvCnt: Int,
    val wndDnvCnt: Int,
    val loCrd: Double,
    val laCrd: Double,
    val geomJson: String
)