#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { CityNotFoundError, describeWeatherCode, getCityWeather } from "./weatherClient.js";
import { recommendClothing } from "./clothingAdvisor.js";

const server = new McpServer({
  name: "mcp-weather",
  version: "1.0.0",
});

function formatWeatherReport(cityQuery: string, city: Awaited<ReturnType<typeof getCityWeather>>["city"], weather: Awaited<ReturnType<typeof getCityWeather>>["weather"]): string {
  const ubicacion = [city.name, city.admin1, city.country].filter(Boolean).join(", ");
  const condicion = describeWeatherCode(weather.weatherCode);
  const franjaHoraria = weather.isDay ? "de día" : "de noche";

  return [
    `Clima actual en ${ubicacion} (consulta: "${cityQuery}"):`,
    `- Hora local: ${weather.localTime}`,
    `- Condición: ${condicion} (${franjaHoraria})`,
    `- Temperatura: ${weather.temperatureC.toFixed(1)}°C (sensación térmica ${weather.apparentTemperatureC.toFixed(1)}°C)`,
    `- Humedad: ${weather.humidityPct}%`,
    `- Precipitación: ${weather.precipitationMm} mm`,
    `- Viento: ${weather.windSpeedKmh.toFixed(1)} km/h`,
  ].join("\n");
}

server.registerTool(
  "get_weather",
  {
    title: "Obtener clima y hora de una ciudad",
    description:
      "Devuelve el clima actual (temperatura, sensación térmica, humedad, viento, condición) y la hora local de una ciudad dada su nombre.",
    inputSchema: {
      city: z
        .string()
        .min(1)
        .describe("Nombre de la ciudad a consultar, ej. 'Santo Domingo' o 'Madrid, España'."),
    },
  },
  async ({ city }) => {
    try {
      const { city: geocoded, weather } = await getCityWeather(city);
      return {
        content: [{ type: "text", text: formatWeatherReport(city, geocoded, weather) }],
      };
    } catch (err) {
      const message = err instanceof CityNotFoundError ? err.message : `Error obteniendo el clima: ${(err as Error).message}`;
      return {
        content: [{ type: "text", text: message }],
        isError: true,
      };
    }
  }
);

server.registerTool(
  "recommend_clothing",
  {
    title: "Recomendar vestimenta según el clima",
    description:
      "Consulta el clima actual de una ciudad y recomienda qué ropa usar (capas, impermeable, protección solar, etc.) según la temperatura, precipitación y viento.",
    inputSchema: {
      city: z
        .string()
        .min(1)
        .describe("Nombre de la ciudad para la cual recomendar vestimenta, ej. 'Santo Domingo'."),
    },
  },
  async ({ city }) => {
    try {
      const { city: geocoded, weather } = await getCityWeather(city);
      const ubicacion = [geocoded.name, geocoded.admin1, geocoded.country].filter(Boolean).join(", ");
      const recomendacion = recommendClothing(weather);
      return {
        content: [{ type: "text", text: `Recomendación de vestimenta para ${ubicacion}:\n\n${recomendacion}` }],
      };
    } catch (err) {
      const message = err instanceof CityNotFoundError ? err.message : `Error obteniendo el clima: ${(err as Error).message}`;
      return {
        content: [{ type: "text", text: message }],
        isError: true,
      };
    }
  }
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  console.error("Error fatal iniciando mcp-weather:", err);
  process.exit(1);
});
