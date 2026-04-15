<template>
  <div class="summary-cards">
    <div class="card income">
      <div class="card-label">本月收入</div>
      <div class="card-amount">¥{{ fmt(summary.totalIncome) }}</div>
    </div>
    <div class="card expense">
      <div class="card-label">本月支出</div>
      <div class="card-amount">¥{{ fmt(summary.totalExpense) }}</div>
    </div>
    <div class="card balance" :class="{ negative: Number(summary.balance) < 0 }">
      <div class="card-label">本月结余</div>
      <div class="card-amount">¥{{ fmt(summary.balance) }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({ summary: Object })

const fmt = (n) =>
  Number(n || 0)
    .toFixed(2)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ',')
</script>

<style scoped>
.summary-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.card {
  flex: 1;
  padding: 20px 24px;
  border-radius: 12px;
  color: #fff;
}

.card-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 8px;
}

.card-amount {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.income {
  background: linear-gradient(135deg, #43a047, #66bb6a);
}

.expense {
  background: linear-gradient(135deg, #e53935, #ef5350);
}

.balance {
  background: linear-gradient(135deg, #1e88e5, #42a5f5);
}

.balance.negative {
  background: linear-gradient(135deg, #fb8c00, #ffa726);
}
</style>
