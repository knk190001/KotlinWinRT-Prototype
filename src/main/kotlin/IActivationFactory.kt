import com.sun.jna.Callback
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.WinDef.UINTByReference
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.platform.win32.WinNT.HANDLEByReference
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

@FieldOrder("iInspectable", "activateInstance")
class IActivationFactory(ptr: Pointer? = Pointer.NULL) : Structure(ptr) {
    init {
        autoRead = true
        read()
    }

    @JvmField
    var iInspectable: IInspectable? = null

    @JvmField
    var activateInstance: ActivateInstance? = null

    interface ActivateInstance : Callback {
        fun invoke(thisPtr: Pointer, returnVal: PointerByReference): HRESULT
    }

    fun activateInstance(thisPtr: Pointer, returnVal: PointerByReference):HRESULT {
        return activateInstance!!.invoke(thisPtr, returnVal)
    }
    companion object {
        var IID = Guid.IID("0000003500000000C000000000000046")
    }
}