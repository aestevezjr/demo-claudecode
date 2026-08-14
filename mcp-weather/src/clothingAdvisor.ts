import { CurrentWeather, describeWeatherCode } from "./weatherClient.js";

/**
 * Lógica basada en reglas para recomendar vestimenta según el clima actual.
 * Usa la temperatura de sensación térmica (apparent temperature) como base,
 * y ajusta por precipitación, viento y si es de día o de noche.
 */
export function recommendClothing(weather: CurrentWeather): string {
  const { apparentTemperatureC: t, precipitationMm, windSpeedKmh, weatherCode, isDay } = weather;
  const items: string[] = [];

  // Capa base según sensación térmica.
  if (t >= 30) {
    items.push("ropa muy ligera y transpirable (franela/camisa de manga corta, shorts o falda)");
  } else if (t >= 25) {
    items.push("ropa ligera de manga corta y telas frescas");
  } else if (t >= 18) {
    items.push("ropa de manga larga liviana o una camisa con una camisa/sudadera fina por si refresca");
  } else if (t >= 10) {
    items.push("suéter o chaqueta ligera");
  } else if (t >= 0) {
    items.push("abrigo grueso, gorro y guantes");
  } else {
    items.push("abrigo térmico de invierno, gorro, bufanda y guantes (frío extremo)");
  }

  // Lluvia / precipitación.
  const isRainy = precipitationMm > 0 || (weatherCode >= 51 && weatherCode <= 82);
  if (isRainy) {
    items.push("paraguas o impermeable");
    items.push("calzado que no se dañe con el agua (evitar tela/gamuza)");
  }

  // Nieve.
  if (weatherCode >= 71 && weatherCode <= 86) {
    items.push("botas impermeables y ropa resistente al agua");
  }

  // Viento fuerte.
  if (windSpeedKmh >= 40) {
    items.push("rompevientos o chaqueta cortavientos");
  }

  // Sol / exposición diurna con temperaturas altas.
  if (isDay && t >= 25 && weatherCode <= 3) {
    items.push("gafas de sol, gorra/sombrero y protector solar");
  }

  const condicion = describeWeatherCode(weatherCode);
  const franjaHoraria = isDay ? "de día" : "de noche";

  const encabezado = `Con ${t.toFixed(1)}°C de sensación térmica y ${condicion} (${franjaHoraria}), se recomienda:`;
  const lista = items.map((i) => `- ${i}`).join("\n");

  return `${encabezado}\n${lista}`;
}
