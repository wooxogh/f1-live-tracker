export interface Session {
  session_key: string;
  session_name: string;
  location: string;
  country_name: string;
}

export interface TrackPoint {
  x: number;
  y: number;
}

export interface DriverPosition {
  driverNumber: number;
  nameAcronym: string;
  teamColour: string;
  x: number;
  y: number;
}

export interface RadioEntry {
  driverNumber: number;
  nameAcronym: string;
  teamColour: string;
  date: string;
  recordingUrl: string;
}

export interface LapData {
  driver_number: number;
  lap_number: number | null;
  duration_sector_1: number | null;
  duration_sector_2: number | null;
  duration_sector_3: number | null;
  lap_duration: number | null;
  segments_sector_1: number[] | null;
  segments_sector_2: number[] | null;
  segments_sector_3: number[] | null;
  s1_status: 'purple' | 'green' | 'yellow' | null;
  s2_status: 'purple' | 'green' | 'yellow' | null;
  s3_status: 'purple' | 'green' | 'yellow' | null;
}
