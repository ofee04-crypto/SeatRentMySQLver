document.addEventListener('DOMContentLoaded', function () {
  const input     = document.getElementById('searchInput');
  const button    = document.getElementById('searchBtn');
  const table     = document.getElementById('ticketTable');
  const priSel    = document.getElementById('priorityFilter');
  const statusSel = document.getElementById('statusFilter');

  if (!table) {
    return; // 找不到表格就直接結束
  }

  const tbody = table.tBodies[0];
  if (!tbody) {
    return;
  }

  function filterTickets() {
    const keyword    = input ? input.value.trim().toLowerCase() : '';
    const priValue   = priSel ? priSel.value.toUpperCase() : '';
    const statusValue= statusSel ? statusSel.value.toUpperCase() : '';
    const rows       = tbody.rows;

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];

      // 「目前沒有維修工單」那一列：第一格有 colspan
      const firstCell = row.cells[0];
      if (!firstCell) {
        continue;
      }
      if (firstCell.hasAttribute('colspan')) {
        continue;
      }

      // 0: ticketId, 2: issueType, 3: issueDesc
      const ticketIdText  = (row.cells[0].textContent || '').toLowerCase();
      const issueTypeText = (row.cells[2].textContent || '').toLowerCase();
      const issueDescText = (row.cells[3].textContent || '').toLowerCase();

      const keywordMatch =
        keyword === '' ||
        ticketIdText.includes(keyword) ||
        issueTypeText.includes(keyword) ||
        issueDescText.includes(keyword);

      // 來自 data-* 屬性（都是大寫）
      const rowPri    = (row.dataset.priority || '').toUpperCase();
      const rowStatus = (row.dataset.status || '').toUpperCase();

      const priMatch    = !priValue   || rowPri === priValue;
      const statusMatch = !statusValue|| rowStatus === statusValue;

      const match = keywordMatch && priMatch && statusMatch;

      row.style.display = match ? '' : 'none';
    }
  }

  if (input) {
    input.addEventListener('keyup', filterTickets);
  }
  if (button) {
    button.addEventListener('click', filterTickets);
  }
  if (priSel) {
    priSel.addEventListener('change', filterTickets);
  }
  if (statusSel) {
    statusSel.addEventListener('change', filterTickets);
  }
});