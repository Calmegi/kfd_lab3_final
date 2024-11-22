import kotlin.system.measureTimeMillis


data class Address(
    val city: String,
    val street: String
)

data class Child(
    val name: String,
    val age: Int
)

data class User(
    val name: String,
    val age: Int,
    val address: Address,
    val children: List<Child>
)


fun main() {
    val user = User(
        name = "Иван Иванов",
        age = 35,
        address = Address(city = "Москва", street = "Тверская"),
        children = List(10000) { index ->
            Child(name = "Child $index", age = 10 + (index % 10))
        }
    )

    val serializer = JsonSerializer()
    val deserializer = JsonDeserializer()

    val serializationTime = measureTimeMillis {
        val json = serializer.serialize(user)
        println("Сериализованный JSON:")
        //println(json)

        val deserializationTime = measureTimeMillis {
            val deserializedUser = deserializer.deserialize(json)
            println("\nДесериализованный объект:")
            //println(deserializedUser)
        }
        println("\nВремя десериализации: $deserializationTime ms")
    }
    println("Время сериализации: $serializationTime ms")


    serializer.shutdown()
    deserializer.shutdown()

    benchmark(user)
}

