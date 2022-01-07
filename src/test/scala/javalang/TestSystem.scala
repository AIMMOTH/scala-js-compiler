package javalang

import org.junit.jupiter.api.{Assertions, DisplayName, Test}

class TestSystem {

  @DisplayName("System properties should be available (JDK 8)")
  @Test
  def test_properties = {
    // Given
    val classPath = "sun.boot.class.path"

    // When
    val classPathProperty = System.getProperty(classPath)

    // Then
    Assertions.assertNotNull(classPathProperty, "If null, you are not using JDK 8")
  }
}
