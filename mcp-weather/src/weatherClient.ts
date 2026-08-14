/**
 * Cliente para la API pública de Open-Meteo (https://open-meteo.com).
 * No requiere API key, por lo que no hay secretos que gestionar.
 */

export interface GeocodedCity {
  name: string;
  country: string;
  admin1?: string;
  latitude: number;
  longitude: number;
  timezone: string;
}

export interface CurrentWeather {
  temperatureC: number;
  apparentTemperatureC: number;
  humidityPct: number;
  precipitationMm: number;
  windSpeedKmh: number;
  weatherCode: number;
  isDay: boolean;
  localTime: string;
}

export interface CityWeather {
  city: GeocodedCity;
  weather: CurrentWeather;
}

const GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
const FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

// Descripciones en español para los códigos WMO que devuelve Open-Meteo.
// https://open-meteo.com/en/docs#weathervariables
const WEATHER_CODE_DESCRIPTIONS: Record<number, string> = {
  0: "cielo despejado",
  1: "mayormente despejado",
  2: "parcialmente nublado",
  3: "nublado",
  45: "niebla",
  48: "niebla con escarcha",
  51: "llovizna ligera",
  53: "llovizna moderada",
  55: "llovizna intensa",
  56: "llovizna helada ligera",
  57: "llovizna helada intensa",
  61: "lluvia ligera",
  63: "lluvia moderada",
  65: "lluvia intensa",
  66: "lluvia helada ligera",
  67: "lluvia helada intensa",
  71: "nevada ligera",
  73: "nevada moderada",
  75: "nevada intensa",
  77: "granos de nieve",
  80: "chubascos ligeros",
  81: "chubascos moderados",
  82: "chubascos violentos",
  85: "chubascos de nieve ligeros",
  86: "chubascos de nieve intensos",
  95: "tormenta eléctrica",
  96: "tormenta con granizo ligero",
  99: "tormenta con granizo intenso",
};

export function describeWeatherCode(code: number): string {
  return WEATHER_CODE_DESCRIPTIONS[code] ?? "condición desconocida";
}

export class CityNotFoundError extends Error {
  constructor(query: string) {
    super(`No se encontró ninguna ciudad que coincida con "${query}".`);
    this.name = "CityNotFoundError";
  }
}

/** Resuelve el nombre de una ciudad a coordenadas usando la API de geocoding de Open-Meteo. */
export async function geocodeCity(query: string): Promise<GeocodedCity> {
  const url = new URL(GEOCODING_URL);
  url.searchParams.set("name", query);
  url.searchParams.set("count", "1");
  url.searchParams.set("language", "es");
  url.searchParams.set("format", "json");

  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Error al geocodificar "${query}": HTTP ${res.status}`);
  }
  const data = (await res.json()) as {
    results?: Array<{
      name: string;
      country: string;
      admin1?: string;
      latitude: number;
      longitude: number;
      timezone: string;
    }>;
  };

  const first = data.results?.[0];
  if (!first) {
    throw new CityNotFoundError(query);
  }

  return {
    name: first.name,
    country: first.country,
    admin1: first.admin1,
    latitude: first.latitude,
    longitude: first.longitude,
    timezone: first.timezone,
  };
}

/** Obtiene el clima actual para unas coordenadas dadas. */
export async function fetchCurrentWeather(
  latitude: number,
  longitude: number,
  timezone: string
): Promise<CurrentWeather> {
  const url = new URL(FORECAST_URL);
  url.searchParams.set("latitude", String(latitude));
  url.searchParams.set("longitude", String(longitude));
  url.searchParams.set(
    "current",
    "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,is_day"
  );
  url.searchParams.set("timezone", timezone);

  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Error al obtener el clima: HTTP ${res.status}`);
  }
  const data = (await res.json()) as {
    current: {
      time: string;
      temperature_2m: number;
      relative_humidity_2m: number;
      apparent_temperature: number;
      precipitation: number;
      weather_code: number;
      wind_speed_10m: number;
      is_day: number;
    };
  };

  const c = data.current;
  return {
    temperatureC: c.temperature_2m,
    apparentTemperatureC: c.apparent_temperature,
    humidityPct: c.relative_humidity_2m,
    precipitationMm: c.precipitation,
    windSpeedKmh: c.wind_speed_10m,
    weatherCode: c.weather_code,
    isDay: c.is_day === 1,
    localTime: c.time,
  };
}

/** Resuelve una ciudad por nombre y devuelve su clima actual. */
export async function getCityWeather(cityQuery: string): Promise<CityWeather> {
  const city = await geocodeCity(cityQuery);
  const weather = await fetchCurrentWeather(city.latitude, city.longitude, city.timezone);
  return { city, weather };
}
