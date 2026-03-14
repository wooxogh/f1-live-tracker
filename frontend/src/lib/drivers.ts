export interface DriverInfo {
  driverNumber: number;
  nameAcronym: string;
  teamName: string;
  teamColour: string;
}

// 2026 시즌 드라이버 목록
export const DRIVERS_2026: DriverInfo[] = [
  { driverNumber: 1,  nameAcronym: 'NOR', teamName: 'McLaren',        teamColour: 'F47600' },
  { driverNumber: 81, nameAcronym: 'PIA', teamName: 'McLaren',        teamColour: 'F47600' },
  { driverNumber: 63, nameAcronym: 'RUS', teamName: 'Mercedes',       teamColour: '00D7B6' },
  { driverNumber: 12, nameAcronym: 'ANT', teamName: 'Mercedes',       teamColour: '00D7B6' },
  { driverNumber: 3,  nameAcronym: 'VER', teamName: 'Red Bull Racing', teamColour: '4781D7' },
  { driverNumber: 6,  nameAcronym: 'HAD', teamName: 'Red Bull Racing', teamColour: '4781D7' },
  { driverNumber: 16, nameAcronym: 'LEC', teamName: 'Ferrari',        teamColour: 'ED1131' },
  { driverNumber: 44, nameAcronym: 'HAM', teamName: 'Ferrari',        teamColour: 'ED1131' },
  { driverNumber: 23, nameAcronym: 'ALB', teamName: 'Williams',       teamColour: '1868DB' },
  { driverNumber: 55, nameAcronym: 'SAI', teamName: 'Williams',       teamColour: '1868DB' },
  { driverNumber: 30, nameAcronym: 'LAW', teamName: 'Racing Bulls',   teamColour: '6C98FF' },
  { driverNumber: 41, nameAcronym: 'LIN', teamName: 'Racing Bulls',   teamColour: '6C98FF' },
  { driverNumber: 14, nameAcronym: 'ALO', teamName: 'Aston Martin',   teamColour: '229971' },
  { driverNumber: 18, nameAcronym: 'STR', teamName: 'Aston Martin',   teamColour: '229971' },
  { driverNumber: 31, nameAcronym: 'OCO', teamName: 'Haas F1 Team',   teamColour: '9C9FA2' },
  { driverNumber: 87, nameAcronym: 'BEA', teamName: 'Haas F1 Team',   teamColour: '9C9FA2' },
  { driverNumber: 11, nameAcronym: 'PER', teamName: 'Cadillac',       teamColour: '909090' },
  { driverNumber: 77, nameAcronym: 'BOT', teamName: 'Cadillac',       teamColour: '909090' },
  { driverNumber: 10, nameAcronym: 'GAS', teamName: 'Alpine',         teamColour: '00A1E8' },
  { driverNumber: 43, nameAcronym: 'COL', teamName: 'Alpine',         teamColour: '00A1E8' },
  { driverNumber: 5,  nameAcronym: 'BOR', teamName: 'Audi',           teamColour: 'F50537' },
  { driverNumber: 27, nameAcronym: 'HUL', teamName: 'Audi',           teamColour: 'F50537' },
];
