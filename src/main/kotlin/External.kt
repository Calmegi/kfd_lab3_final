import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.system.measureTimeMillis
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.json.JSONObject
import kotlin.system.measureTimeMillis


class JsonSerializer {
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun serialize(user: User): String {
        val addressTask: Callable<String> = Callable { serializeAddress(user.address) }
        val childrenTask: Callable<List<String>> = Callable { user.children.map { serializeChild(it) } }

        val addressFuture: Future<String> = executor.submit(addressTask)
        val childrenFuture: Future<List<String>> = executor.submit(childrenTask)

        val addressJson = addressFuture.get()
        val childrenJsonList = childrenFuture.get()

        val userJson = """
            {
                "name": "${user.name}",
                "age": ${user.age},
                "address": $addressJson,
                "children": [${childrenJsonList.joinToString(",")}]
            }
        """.trimIndent()

        return userJson
    }

    private fun serializeAddress(address: Address): String {
        return """
            {
                "city": "${address.city}",
                "street": "${address.street}"
            }
        """.trimIndent()
    }

    private fun serializeChild(child: Child): String {
        return """
            {
                "name": "${child.name}",
                "age": ${child.age}
            }
        """.trimIndent()
    }

    fun shutdown() {
        executor.shutdown()
    }
}


class JsonDeserializer {
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun deserialize(json: String): User {
        val jsonObject = JSONObject(json)

        val name = jsonObject.getString("name")
        val age = jsonObject.getInt("age")
        val addressJson = jsonObject.getJSONObject("address")
        val childrenJsonArray = jsonObject.getJSONArray("children")

        val addressTask: Callable<Address> = Callable { deserializeAddress(addressJson) }
        val childrenTask: Callable<List<Child>> = Callable {
            (0 until childrenJsonArray.length()).map { index ->
                deserializeChild(childrenJsonArray.getJSONObject(index))
            }
        }

        val addressFuture: Future<Address> = executor.submit(addressTask)
        val childrenFuture: Future<List<Child>> = executor.submit(childrenTask)

        val address = addressFuture.get()
        val children = childrenFuture.get()

        return User(name, age, address, children)
    }

    private fun deserializeAddress(jsonObject: JSONObject): Address {
        val city = jsonObject.getString("city")
        val street = jsonObject.getString("street")
        return Address(city, street)
    }

    private fun deserializeChild(jsonObject: JSONObject): Child {
        val name = jsonObject.getString("name")
        val age = jsonObject.getInt("age")
        return Child(name, age)
    }

    fun shutdown() {
        executor.shutdown()
    }
}


fun benchmark(user: User) {
    val gson = GsonBuilder().create()
    val jackson = jacksonObjectMapper()

    var gsonSerializationTime = 0L
    var gsonDeserializationTime = 0L
    var gsonJson = ""
    gsonSerializationTime = measureTimeMillis {
        gsonJson = gson.toJson(user)
    }
    gsonDeserializationTime = measureTimeMillis {
        gson.fromJson(gsonJson, User::class.java)
    }


    var jacksonSerializationTime = 0L
    var jacksonDeserializationTime = 0L
    var jacksonJson = ""
    jacksonSerializationTime = measureTimeMillis {
        jacksonJson = jackson.writeValueAsString(user)
    }
    jacksonDeserializationTime = measureTimeMillis {
        jackson.readValue(jacksonJson, User::class.java)
    }

    println("Gson Serialization Time: $gsonSerializationTime ms")
    println("Gson Deserialization Time: $gsonDeserializationTime ms")
    println("Jackson Serialization Time: $jacksonSerializationTime ms")
    println("Jackson Deserialization Time: $jacksonDeserializationTime ms")
}
