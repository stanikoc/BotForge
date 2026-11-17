plugins {
    application
}

dependencies {
    implementation(project(":botforge"))
}

application {
    mainModule = "io.github.stanikoc.botforge.example"
    mainClass = "io.github.stanikoc.botforge.example.ExampleBot"
}
