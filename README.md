# VisualScalePlugin v1.1.0 — Código Fuente

## ¿Qué se corrigió?

El problema original era que `NmsScaleHelper` intentaba localizar el atributo
`generic.scale` mediante reflexión NMS directa, lo que fallaba en Paper 1.20.1
porque el campo `RangedAttribute` no tiene el nombre esperado con los mapeos Mojang.

**Solución:** La clase `NmsScaleHelper` fue reescrita para usar **ProtocolLib** 
directamente (`WrappedAttribute` + `PacketType.Play.Server.UPDATE_ATTRIBUTES`)
en lugar de reflexión NMS frágil. Esto elimina todos los WARNs del log:

```
[WARN] [Scale] NMS falló: net.minecraft.world.entity.ai.attributes.RangedAttribute
[WARN] [Scale] MODO DATOS activo — escalas guardadas pero no se envían paquetes visuales.
```

## Requisitos

- Java 17+
- Maven 3.6+
- Conexión a internet (para descargar Paper API y ProtocolLib en el primer build)

## Compilar

```bash
mvn package
```

El JAR resultante estará en `target/VisualScalePlugin-1.1.0.jar`.

## Instalar

1. Copia `target/VisualScalePlugin-1.1.0.jar` a la carpeta `plugins/` de tu servidor.
2. Asegúrate de tener `ProtocolLib.jar` también en `plugins/`.
3. Reinicia el servidor.

## Verificar que funciona

En el log debes ver:
```
[INFO] [Scale] NmsScaleHelper inicializado con ProtocolLib.
[INFO] VisualScalePlugin habilitado.
[INFO]   ProtocolLib: true
```

Ya NO deben aparecer los mensajes de "NMS falló" ni "MODO DATOS activo".
