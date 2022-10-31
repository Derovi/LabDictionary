package by.derovi.botp2p.lang

fun main(){
    (readln()+' '+readln()).split(' ').map { it.toInt() }.let { e->
        print(e.drop(2).count{it>=e[e[1]+1]&&it>0})
    }
}
