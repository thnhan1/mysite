<template>
    <div class="ms-demo__content">
        xin chào uuuuu {{ name }}
    </div>
</template>

<script>
export default {
    name: 'MsDemo',
    props: {
        // Nhận dữ liệu từ data-name attribute trong HTL
        name: {
            type: String,
            default: 'Nguyen Van A',
        },
    },
    mounted() {
        console.log('[ms-demo] mounted with name:', this.name);
    },
};
</script>

<style scoped>
.ms-demo__content {
    padding: 1rem;
    border: 2px solid #42b883;
    border-radius: 6px;
    color: #35495e;
}
</style>
