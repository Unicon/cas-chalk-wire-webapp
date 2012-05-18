#CAS Authentication for Chalk & Wire

`cas-chalk-wire-webapp` is a small application that allows a CAS user to log into Chalk & Wire. 
The module is designed as a small `HttpServlet` wrapped around the CAS Java client and that submits the userId to
Chalk & Wire to receive the single sign-on token and uses that to generate the final single sign-on url. 

##Configuration
See the `chalkwire.properties` for the list of configuration keys.

##Deploy
Use the ANT command: `ant deploy` on the command prompt.