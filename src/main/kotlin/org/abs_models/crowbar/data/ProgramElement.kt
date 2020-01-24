package org.abs_models.crowbar.data

import kotlin.reflect.KClass


interface ProgramElement: Anything{
    fun<T : Any> collectAll(clazz : KClass<T>) : Set<ProgramElement> {
        return if(clazz.isInstance(this)) setOf(this)
        else emptySet()
    }
}
data class ProgramElementAbstractVar(val name : String) : ProgramElement, AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}


interface PP: ProgramElement
data class PPId(val id: Int): PP, ProgramElement {
    override fun prettyPrint(): String {
        return "pp:$id"
    }
}

data class PPAbstractVar(val name : String) : PP, AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}

interface Stmt: ProgramElement {
    fun hasReturn(): Boolean = false
}

data class StmtAbstractVar(val name : String) : Stmt, AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}
data class AssignStmt(val lhs : Location, val rhs : Expr) : Stmt {
    override fun prettyPrint(): String {
        return lhs.prettyPrint()+" = "+rhs.prettyPrint()
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + lhs.collectAll(clazz) + rhs.collectAll(clazz)
    }
}
data class AllocateStmt(val lhs : Location, val rhs : Expr) : Stmt {
    override fun prettyPrint(): String {
        return lhs.prettyPrint()+" = new "+rhs.prettyPrint()
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + lhs.collectAll(clazz) + rhs.collectAll(clazz)
    }
}
data class SyncStmt(val lhs : Location, val rhs : Expr) : Stmt {
    override fun prettyPrint(): String {
        return lhs.prettyPrint()+" =  "+rhs.prettyPrint()+".get"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + lhs.collectAll(clazz) + rhs.collectAll(clazz)
    }
}
object SkipStmt : Stmt {
    override fun prettyPrint(): String {
        return "skip"
    }
}
data class SeqStmt(val first : Stmt, val second : Stmt) : Stmt {
    override fun prettyPrint(): String {
        return first.prettyPrint()+";"+second.prettyPrint()
    }

    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + first.collectAll(clazz) + second.collectAll(clazz)
    }
    override fun hasReturn(): Boolean = first.hasReturn() || second.hasReturn()
}
data class ReturnStmt(val resExpr : Expr) : Stmt {
    override fun prettyPrint(): String {
        return "return "+resExpr.prettyPrint()
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + resExpr.collectAll(clazz)
    }
    override fun hasReturn(): Boolean = true
}

data class IfStmt(val guard : Expr, val thenStmt : Stmt, val elseStmt : Stmt) : Stmt {
    override fun prettyPrint(): String {
        return "if( ${guard.prettyPrint()} ){ ${thenStmt.prettyPrint()} } else { ${elseStmt.prettyPrint()} }"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + guard.collectAll(clazz) + thenStmt.collectAll(clazz) + elseStmt.collectAll(clazz)
    }
    override fun hasReturn(): Boolean = thenStmt.hasReturn() || elseStmt.hasReturn()
}

data class WhileStmt(val guard : Expr, val bodyStmt : Stmt, val id : PP, val invar : Formula = True) : Stmt {
    override fun prettyPrint(): String {
        return "while{${id.prettyPrint()}}( ${guard.prettyPrint()} ){ ${bodyStmt.prettyPrint()} }"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + guard.collectAll(clazz) + bodyStmt.collectAll(clazz)
    }
    override fun hasReturn(): Boolean = bodyStmt.hasReturn()
}

data class AwaitStmt(val resExpr : Expr, val id : PP) : Stmt {
    override fun prettyPrint(): String {
        return "await{${id.prettyPrint()}} "+resExpr.prettyPrint()
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + resExpr.collectAll(clazz)
    }
}

data class CallStmt(val lhs : Location, val target : Expr, val resExpr : CallingExpr) : Stmt {
    override fun prettyPrint(): String {
        return "${lhs.prettyPrint()} = ${target.prettyPrint()}.${resExpr.prettyPrint()}"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + target.collectAll(clazz) + resExpr.collectAll(clazz)
    }
}




interface Expr : ProgramElement
data class ExprAbstractVar(val name : String) : Expr, AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}
interface CallingExpr : Expr
data class CallExprAbstractVar(val name : String) : CallingExpr, AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}
data class CallExpr(val met : String, val e : List<Expr>) : CallingExpr{
    override fun prettyPrint(): String {
        return met+"("+e.map { p -> p.prettyPrint() }.fold("", { acc, nx -> "$acc,$nx" }).removePrefix(",") + ")"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return e.fold(super.collectAll(clazz), {acc, nx -> acc + nx.collectAll(clazz)})
    }
}

data class AddExpr(val e1 : Expr, val e2 : Expr) : Expr {
    override fun prettyPrint(): String {
        return e1.prettyPrint()+"+"+e2.prettyPrint()
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + e1.collectAll(clazz) + e2.collectAll(clazz)
    }
}
data class PollExpr(val e1 : Expr) : Expr {
    override fun prettyPrint(): String {
        return e1.prettyPrint()+"?"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return super.collectAll(clazz) + e1.collectAll(clazz)
    }
}
data class SExpr(val op : String, val e : List<Expr>) : Expr {
    override fun prettyPrint(): String {
        if(e.isEmpty()) return op
        return op+"("+e.map { p -> p.prettyPrint() }.fold("", { acc, nx -> "$acc,$nx" }).removePrefix(",") + ")"
    }
    override fun <T : Any> collectAll(clazz: KClass<T>): Set<ProgramElement> {
        return e.fold(super.collectAll(clazz), {acc, nx -> acc + nx.collectAll(clazz)})
    }
}
data class Const(val name : String)  : Expr {
    override fun prettyPrint(): String {
        return name
    }
}

interface Location : Expr
data class LocationAbstractVar(val name : String) : Location, AbstractVar{
    override fun prettyPrint(): String {
        return name
    }
}
open class Field(val name : String, val dType : String = "Int") : Location, Term {
    override fun prettyPrint(): String {
        return "this.$name : $dType"
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Field

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
    override fun getFields() : Set<Field> =setOf(this)
    override fun getProgVars() : Set<ProgVar> =  emptySet()
    override fun getHeapNews() : Set<String> =  emptySet()
    override fun toSMT(isInForm : Boolean) : String = name
}

open class ProgVar(val name : String, val dType : String = "Int") : Location, Term { //todo: change simpleName to qualifiedName and do something clever in the SMT-translation
    override fun prettyPrint(): String {
        return "$name:$dType"
    }

    //this ignores the type and that is ok for now
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProgVar

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
    override fun getFields() : Set<Field> = emptySet()
    override fun getProgVars() : Set<ProgVar> = setOf(this)
    override fun getHeapNews() : Set<String> =  emptySet()
    override fun toSMT(isInForm : Boolean) : String = name
}
data class ReturnVar(val vParam : String) : ProgVar("result", vParam)

data class ProgAbstractVar(val vName : String) : ProgVar(vName, "AVAR"), AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
}
data class ProgFieldAbstractVar(val vName : String) : Field(vName, "AVAR"), AbstractVar {
    override fun prettyPrint(): String {
        return name
    }
    override fun getFields() : Set<Field> = emptySet()
    override fun getProgVars() : Set<ProgVar> = emptySet()
    override fun toSMT(isInForm : Boolean) : String = name
}

fun appendStmt(stmt : Stmt, add : Stmt) : Stmt {
    return when(stmt){
        is SeqStmt -> {
            val (first, next) = stmt
            SeqStmt(first,appendStmt(next,add))
        }
        else -> SeqStmt(stmt, add)
    }
}

fun unitExpr() : Expr = SExpr("Unit", emptyList())
