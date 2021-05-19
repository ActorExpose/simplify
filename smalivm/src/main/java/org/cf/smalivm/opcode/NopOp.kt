package org.cf.smalivm.opcode

import org.cf.smalivm.configuration.Configuration
import org.cf.smalivm.dex.SmaliClassLoader
import org.cf.smalivm.type.ClassManager
import org.cf.smalivm.ExecutionNode
import org.cf.smalivm.UnresolvedChild
import org.jf.dexlib2.builder.MethodLocation

class NopOp internal constructor(location: MethodLocation) : Op(location) {

    override val registersReadCount = 0
    override val registersAssignedCount = 0

    override fun execute(node: ExecutionNode): Array<out UnresolvedChild> {
        // Yesterday, upon the stair,
        // I met an op who wasn't there.
        // It wasn't there again today,
        // I wish, I wish it'd go away...
        //
        // Last night I saw upon the stair,
        // A little op who wasn't there,
        // It wasn't there again today
        // Oh, how I wish it'd go away...
        return finishOp()
    }

    override fun toString() = name

    companion object : OpFactory {
        override fun build(
            location: MethodLocation,
            classManager: ClassManager,
            classLoader: SmaliClassLoader,
            configuration: Configuration
        ): Op {
            return NopOp(location)
        }
    }
}
