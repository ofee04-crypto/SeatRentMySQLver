
const fs = require('fs');
const path = require('path');

const filePath = 'c:\\Labworks\\JavaLab\\SeatRent\\backend\\db\\all\\第三組SQL 最新版本資料.sql';

function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function formatDate(date) {
  const pad = (num) => num.toString().padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  const second = pad(date.getSeconds());
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

try {
    let sqlContent = fs.readFileSync(filePath, 'utf8');

    const recRentInsertRegex = /(INSERT INTO recRent[\s\S]*?VALUES\s*)([\s\S]*?)(?=--SQL VIEWS|CREATE VIEW|--==================子桓 DATA END====================)/i;

    let foundBlock = false;
    const newSqlContent = sqlContent.replace(recRentInsertRegex, (match, insertPrefix, valuesBlock) => {
        foundBlock = true;
        
        const lines = valuesBlock.split('\n');
        
        const updatedLines = lines.map(line => {
            if (line.trim().startsWith('--') || line.trim() === '') {
                return line;
            }

            const valueRegex = /'(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2})',\s*'(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2})'/;
            const parts = line.match(valueRegex);

            if (parts) {
                const year = getRandomInt(2021, 2025);
                const month = getRandomInt(0, 11);
                const day = getRandomInt(1, 28);
                const hour = getRandomInt(0, 23);
                const minute = getRandomInt(0, 59);
                const second = getRandomInt(0, 59);
                const startDate = new Date(year, month, day, hour, minute, second);

                const durationMinutes = getRandomInt(30, 5 * 24 * 60); // 30 mins to 5 days
                const endDate = new Date(startDate.getTime() + durationMinutes * 60000);

                const newStartDateStr = formatDate(startDate);
                const newEndDateStr = formatDate(endDate);

                const originalDatePair = parts[0];
                const newDatePair = `'${newStartDateStr}', '${newEndDateStr}'`;

                return line.replace(originalDatePair, newDatePair);
            }
            return line;
        });

        return insertPrefix + updatedLines.join('\n');
    });

    if (foundBlock) {
        fs.writeFileSync(filePath, newSqlContent, 'utf8');
        console.log('SQL file has been updated successfully.');
    } else {
        console.log('No matching INSERT INTO recRent statement found to modify.');
    }

} catch (error) {
    console.error('An error occurred:', error);
    process.exit(1);
}
