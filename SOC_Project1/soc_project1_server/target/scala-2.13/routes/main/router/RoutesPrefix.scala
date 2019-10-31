// @GENERATOR:play-routes-compiler
// @SOURCE:C:/Users/mukht/Desktop/SOC_Project1/soc_project1_server/conf/routes
// @DATE:Sat Sep 14 00:19:37 EDT 2019


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
